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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.function.Consumer

@Component(value = "iotData")
class IotDataConsumer(
    private val fingerprintService: FingerprintService,
    private val recommendationService: RecommendationService
) : Consumer<Rtl433Message> {

    private val log by LoggerDelegate()

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun accept(message: Rtl433Message) {
        log.debug("message - {}", message)
        scope.launch {
            try {
                processMessage(message).awaitSingleOrNull()
            } catch (ex: Exception) {
                log.error("Error processing message {}", message, ex)
            }
        }
    }

    private fun processMessage(msg: Rtl433Message): Mono<Recommendation> {
        val payload = msg.payload
        val propSample = payload.getProperties()
        return fingerprintService.registerObservation(payload)
            .flatMap { (fingerprint, bucketCount) ->
                recommendationService.maybeCreateRecommendation(fingerprint, payload.model, payload.id, bucketCount, propSample)
            }
    }

}
