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

package io.jrb.labs.recommendationms.messaging

import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.messages.Rtl433Message
import io.jrb.labs.recommendationms.model.Recommendation
import io.jrb.labs.recommendationms.service.FingerprintService
import io.jrb.labs.recommendationms.service.RecommendationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class IotDataConsumer(
    private val fingerprintService: FingerprintService,
    private val recommendationService: RecommendationService
) {

    private val log by LoggerDelegate()

    @Bean
    fun iotData(): suspend (Flow<Rtl433Message>) -> Unit = { flow ->
        flow.onEach { msg ->
            try {
                processMessage(msg)
            } catch (e: Exception) {
                log.error("Error processing message ${msg.id}", e)
            }
        }.collect()
    }

    private suspend fun processMessage(msg: Rtl433Message): Recommendation? {
        log.debug("message - {}", msg)
        val payload = msg.payload
        val deviceId = payload.id
        val propertiesSample = payload.getProperties()

        val (fingerprint, bucketCount) = fingerprintService.registerObservation(payload)

        return recommendationService.maybeCreateRecommendation(
            fingerprint = fingerprint,
            model = payload.model,
            deviceId = deviceId,
            bucketCount = bucketCount,
            propertiesSample = propertiesSample
        )
    }

}
