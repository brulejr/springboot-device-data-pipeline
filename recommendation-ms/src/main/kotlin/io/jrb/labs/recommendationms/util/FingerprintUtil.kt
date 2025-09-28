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
package io.jrb.labs.recommendationms.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

object FingerprintUtil {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    /**
     * Produces a deterministic fingerprint string for the device properties + model + id
     */
    fun fingerprintFor(model: String, deviceId: String, properties: Map<String, Any?>): String {
        // Sort keys and create deterministic JSON
        val sortedMap = properties.toSortedMap()
        val base = mapOf(
            "model" to model,
            "deviceId" to deviceId,
            "props" to sortedMap
        )
        val json = mapper.writeValueAsString(base)
        val digest = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun bucketStartEpochMinutes(now: Instant, bucketMinutes: Long): Long {
        val epochMin = now.epochSecond / 60
        val bucket = (epochMin / bucketMinutes) * bucketMinutes
        return bucket
    }

}
