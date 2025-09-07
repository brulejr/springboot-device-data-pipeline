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

import java.lang.Exception
import java.time.Duration
import kotlin.also
import kotlin.let

open class LoadingL1L2Cache<K : CacheKey, V : CacheEntry<V>>(
    private val l1Cache: L1Cache<K, V>,
    private val store: CacheStore<K, V>,
    private val ttlL1Cache: Duration,
    private val ttlL2Cache: Duration,
    private val loadingFn: suspend (K) -> V? = { null },
    private val fallbackFn: suspend (K) -> V? = { null }
) : LoadingCache<K, V> {

    override suspend fun get(key: K): V? {
        return l1Cache.get(key)
            ?: store.get(key)?.also { l1Cache.put(key, it, ttlL1Cache) }
            ?: try {
                if (key.hasMinimumLoadingCriteria()) {
                    loadingFn(key)?.let { value ->
                        l1Cache.put(key, value, ttlL1Cache)
                        store.put(key, value, ttlL2Cache)
                    }
                } else null
            } catch(_: Exception) { null }
            ?: fallbackFn(key)?.let { value ->
                l1Cache.put(key, value, ttlL1Cache)
                store.put(key, value, ttlL2Cache)
            }
    }

    override suspend fun put(key: K, value: V, ttl: Duration?) {
        l1Cache.put(key, value, ttlL1Cache)
        store.put(key, value, ttl ?: this.ttlL2Cache)
    }

    override suspend fun invalidate(key: K) {
        l1Cache.invalidate(key)
        store.invalidate(key)
    }

}
