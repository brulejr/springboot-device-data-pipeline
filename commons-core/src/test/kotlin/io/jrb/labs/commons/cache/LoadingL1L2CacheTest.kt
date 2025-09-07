/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.commons.cache

import io.jrb.labs.commons.test.TestUtils
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadingL1L2CacheTest : TestUtils {

    private lateinit var l1Cache: L1Cache<TestKey, TestEntry>
    private lateinit var loadingCache: LoadingL1L2Cache<TestKey, TestEntry>
    private lateinit var store: CacheStore<TestKey, TestEntry>

    private val ttlL1Cache = Duration.ofMinutes(10)
    private val ttlL2Cache = Duration.ofMinutes(15)

    data class TestScenario<T>(
        val name: String,
        val setup: suspend (String, String) -> TestKey,
        val expected: suspend (String, T?) -> Unit
    )

    private val loadingFn: suspend (TestKey) -> TestEntry? = spyk({ TestEntry(randomString()) })
    private val fallbackFn: suspend (TestKey) -> TestEntry? = spyk({ TestEntry(randomString()) })

    @BeforeEach
    fun setup() {
        store = mockk(relaxed = true)
        l1Cache = spyk(InMemoryL1Cache())
        loadingCache = LoadingL1L2Cache(
            l1Cache = l1Cache,
            store = store,
            ttlL1Cache = ttlL1Cache,
            ttlL2Cache = ttlL2Cache,
            loadingFn = loadingFn,
            fallbackFn = fallbackFn
        )
    }

    @AfterEach
    fun tearDown() {
        clearMocks(l1Cache, store, loadingFn, fallbackFn)
    }

    fun scenarios(): Stream<TestScenario<TestEntry>> = Stream.of(
        TestScenario(
            name = "L1 cache hit",
            setup = { key, value ->
                val cacheKey = TestKey(id = key)
                val cacheValue = TestEntry(value = value)
                l1Cache.put(cacheKey, cacheValue, ttlL1Cache)
                cacheKey
            },
            expected = { expectedValue, result  ->
                assertThat(result).isNotNull.extracting("value").isEqualTo(expectedValue)
                coVerify(exactly = 1) { l1Cache.get(any()) }
                coVerify(exactly = 0) { store.get(any()) }
                coVerify(exactly = 0) { loadingFn(any()) }
                coVerify(exactly = 0) { fallbackFn(any()) }
            }
        ),
        TestScenario(
            name = "L2 cache storage hit due to L1 cache miss",
            setup = { key, value ->
                val cacheKey = TestKey(id = key)
                val cacheValue = TestEntry(value = value)
                coEvery { store.get(cacheKey) } returns cacheValue
                l1Cache.invalidate(cacheKey)
                cacheKey
            },
            expected = { expectedValue, result  ->
                assertThat(result).isNotNull.extracting("value").isEqualTo(expectedValue)
                coVerify(exactly = 1) { l1Cache.get(any()) }
                coVerify(exactly = 1) { store.get(any()) }
                coVerify(exactly = 0) { loadingFn(any()) }
                coVerify(exactly = 0) { fallbackFn(any()) }
            }
        ),
        TestScenario(
            name = "L2 cache storage hit due to L1 cache expiration",
            setup = { key, value ->
                val cacheKey = TestKey(id = key)
                val cacheValue = TestEntry(value = value)
                coEvery { store.get(cacheKey) } returns cacheValue
                l1Cache.invalidate(cacheKey)
                cacheKey
            },
            expected = { expectedValue, result  ->
                assertThat(result).isNotNull.extracting("value").isEqualTo(expectedValue)
                coVerify(exactly = 1) { l1Cache.get(any()) }
                coVerify(exactly = 1) { store.get(any()) }
            }
        ),
        TestScenario(
            name = "Cache miss leading to loading function call",
            setup = { key, value ->
                val cacheKey = TestKey(id = key, valid = true)
                val cacheValue = TestEntry(value = value)
                coEvery { store.get(cacheKey) } returns null
                coEvery { store.put(cacheKey, any(), any()) } returns cacheValue
                l1Cache.invalidate(cacheKey)
                cacheKey
            },
            expected = { expectedValue, result  ->
                assertThat(result).isNotNull.extracting("value").isEqualTo(expectedValue)
                coVerify(exactly = 1) { l1Cache.get(any()) }
                coVerify(exactly = 1) { store.get(any()) }
                coVerify(exactly = 1) { loadingFn(any()) }
                coVerify(exactly = 0) { fallbackFn(any()) }
            }
        ),
        TestScenario(
            name = "Cache miss leading to fallback function call",
            setup = { key, value ->
                val cacheKey = TestKey(id = key, valid = false)
                val cacheValue = TestEntry(value = value)
                coEvery { store.get(cacheKey) } returns null
                coEvery { store.put(cacheKey, any(), any()) } returns cacheValue
                l1Cache.invalidate(cacheKey)
                cacheKey
            },
            expected = { expectedValue, result  ->
                assertThat(result).isNotNull.extracting("value").isEqualTo(expectedValue)
                coVerify(exactly = 1) { l1Cache.get(any()) }
                coVerify(exactly = 1) { store.get(any()) }
                coVerify(exactly = 0) { loadingFn(any()) }
                coVerify(exactly = 1) { fallbackFn(any()) }
            }
        )
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    fun `test different cache scenarios`(scenario: TestScenario<TestEntry>) = runTest {

        // given
        val key = randomString()
        val value = randomString()
        val cacheKey = scenario.setup.invoke(key, value)

        // when
        val result = loadingCache.get(cacheKey)

        // then
        scenario.expected.invoke(value, result)
    }

}
