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

import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.recommendationms.datafill.RecommendationDatafill
import io.jrb.labs.recommendationms.model.Recommendation
import io.jrb.labs.recommendationms.repository.RecommendationRepository
import io.jrb.labs.recommendationms.resource.RecommendationResource
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Instant

@Service
class RecommendationService(
    private val repository: RecommendationRepository,
    private val datafill: RecommendationDatafill
) {

    private val log by LoggerDelegate()

    suspend fun maybeCreateRecommendation(
        fingerprint: String,
        model: String,
        deviceId: String,
        bucketCount: Long,
        propertiesSample: Map<String, Any?>
    ): Recommendation? {
        if (bucketCount < datafill.bucketCountThreshold) return null

        val now = Instant.now()
        val existing = repository.findByFingerprint(fingerprint).awaitFirstOrNull()

        val recommendation = if (existing == null) {
            Recommendation(
                id = null,
                fingerprint = fingerprint,
                model = model,
                deviceId = deviceId,
                firstSeen = now,
                lastSeen = now,
                bucketCount = bucketCount,
                propertiesSample = propertiesSample
            )
        } else {
            existing.copy(lastSeen = now, bucketCount = bucketCount)
        }

        log.info("Recommendation -> {}", recommendation)
        return repository.save(recommendation).awaitFirstOrNull()
    }

    fun listCandidates(): Flux<RecommendationResource> {
        return repository.findAllByPromotedIsFalse().map { it.toRecommendationResource() }
    }

}
