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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryL1CacheTest : TestUtils {

    private lateinit var cache: InMemoryL1Cache<TestKey, TestEntry>

    data class TestScenario(
        val name: String,
        val setup: suspend (InMemoryL1Cache<TestKey, TestEntry>, String, String) -> TestKey,
        val expected: suspend (String, TestEntry?) -> Unit
    )

    @BeforeEach
    fun setUp() {
        cache = InMemoryL1Cache()
    }

    companion object {
        @JvmStatic
        fun scenarios(): Stream<TestScenario> = Stream.of(
            TestScenario(
                name = "cache miss",
                setup = { _, key, _ -> TestKey(id = key) },
                expected = { _, result -> assertThat(result).isNull() }
            ),
            TestScenario(
                name = "cache hit",
                setup = { cache, key, value ->
                    val cacheKey = TestKey(id = key)
                    val cacheValue = TestEntry(value = value)
                    cache.put(cacheKey, cacheValue, Duration.ofSeconds(60))
                    cacheKey
                },
                expected = { expectedValue, result -> assertThat(result).isNotNull.extracting("value").isEqualTo(expectedValue) }
            ),
            TestScenario(
                name = "cache expired",
                setup = { cache, key, value ->
                    val cacheKey = TestKey(id = key)
                    val cacheValue = TestEntry(value = value)
                    cache.put(cacheKey, cacheValue, Duration.ZERO)
                    cacheKey
                },
                expected = { _, result -> assertThat(result).isNull() }
            )
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    fun `test cache get scenarios`(scenario: TestScenario) = runTest {

        // given
        val key = randomString()
        val value = randomString()
        val cacheKey = scenario.setup.invoke(cache, key, value)

        // when
        val result = cache.get(cacheKey)

        // then
        scenario.expected(value, result)
    }

}
