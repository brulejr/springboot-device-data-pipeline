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
package io.jrb.labs.ingester.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.logging.LoggerDelegate
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.datatypes.Rtl433Data
import io.jrb.labs.ingester.data.Source
import io.jrb.labs.ingester.datafill.IngesterDatafill
import io.jrb.labs.messages.RawMessageSource
import io.jrb.labs.messages.Rtl433Message
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import reactor.core.Disposable

@Service
class Rtl433Ingester(
    private val sources: List<Source>,
    private val objectMapper: ObjectMapper,
    private val streamBridge: StreamBridge,
    private val datafill: IngesterDatafill,
    systemEventBus: SystemEventBus
): ControllableService(systemEventBus) {

    private val log by LoggerDelegate()

    private val _subscriptions: MutableMap<String, Disposable?> = mutableMapOf()

    override fun onStart() {
        sources.forEach { source ->
            source.connect()
            _subscriptions[source.name] = source.subscribe(source.topic) { message ->
                val rtl433Data = objectMapper.readValue(message, Rtl433Data::class.java)
                streamBridge.send(
                    datafill.outboundTopicBinding,
                    Rtl433Message(source = RawMessageSource.RTL433, payload = rtl433Data)
                ).let { status -> log.info("message - sent = $status, message - $rtl433Data") }
            }
        }
    }

    override fun onStop() {
        sources.forEach { source ->
            _subscriptions.remove(source.name).let { subscription ->
                subscription?.dispose()
            }
            source.disconnect()
        }
    }

}