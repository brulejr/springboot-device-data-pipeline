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
package io.jrb.labs.recommendationms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.reactivecircus.cache4k.Cache
import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.datatypes.Rtl433Data
import io.jrb.labs.recommendationms.datafill.RecommendationDatafill
import io.jrb.labs.recommendationms.model.FingerprintCount
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class FingerprintService(
    private val mongo: ReactiveMongoTemplate,
    private val datafill: RecommendationDatafill
) {
    private val log by LoggerDelegate()

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val cache = Cache.Builder()
        .expireAfterWrite(datafill.dedupe.cacheTtlSeconds.toDuration(DurationUnit.SECONDS))
        .maximumCacheSize(datafill.dedupe.cacheMaxSize)
        .build<String, Boolean>()

    /**
     * Register a single observation. Returns the new bucket count (after increment).
     * Uses in-memory cache to suppress frequent DB writes for the same fingerprint within TTL.
     */
    fun registerObservation(data: Rtl433Data): Mono<Pair<String, Long>> {

        val now = Instant.now()
        val fingerprint = fingerprintFor(data)

        val bucketStart = bucketStartEpochMinutes(now, datafill.fingerprint.bucketDurationMinutes)
        val key = "$fingerprint#$bucketStart"

        // If seen recently in cache -> skip DB write and return current count from DB (non-blocking)
        return mono {
            val recent = cache.get(key)
            val count = when (recent) {
                true -> {
                    // fetch current count from DB (no update)
                    val q = Query.query(Criteria.where("_id").`is`(key))
                    val existing = mongo.findById(key, FingerprintCount::class.java).awaitFirstOrNull()
                    existing?.count ?: 0L
                }
                else -> {
                    // mark in cache and upsert DB (atomic increment)
                    cache.put(key, true)
                    val q = Query.query(Criteria.where("_id").`is`(key))
                    val update = Update()
                        .inc("count", 1)
                        .setOnInsert("fingerprint", fingerprint)
                        .setOnInsert("bucketStartEpoch", bucketStart)

                    // Use upsert
                    val result = mongo.upsert(q, update, FingerprintCount::class.java).awaitFirstOrNull()

                    // fetch post-upsert value (atomic read)
                    val post = mongo.findById(key, FingerprintCount::class.java).awaitFirstOrNull()
                    post?.count ?: 1L
                }
            }
            log.info("registerObservation - fingerprint='{}', bucketStart='{}', count='{}'", fingerprint, bucketStart, count)
            Pair(fingerprint, count)
        }
    }

    fun bucketStartEpochMinutes(now: Instant, bucketMinutes: Long): Long {
        val epochMin = now.epochSecond / 60
        val bucket = (epochMin / bucketMinutes) * bucketMinutes
        return bucket
    }

    fun fingerprintFor(data: Rtl433Data): String {
        val base = mapOf(
            "model" to data.model,
            "deviceId" to data.id
        )
        val json = mapper.writeValueAsString(base)
        val digest = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

}
