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
package io.jrb.labs.modelms.client

import io.jrb.labs.commons.client.ResourceWrapper
import io.jrb.labs.messages.Rtl433Message
import io.jrb.labs.resources.model.ModelResource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.createExceptionAndAwait

class ModelClient(
    private val datafill: ModelClientDatafill,
    private val webClient: WebClient
) {

    suspend fun searchModel(rtl433Message: Rtl433Message): SearchResult {
        return try {
            webClient.post()
                .uri(datafill.searchEndpoint)
                .bodyValue(rtl433Message)
                .awaitExchange { response ->
                    when (response.statusCode()) {
                        HttpStatus.OK -> {
                            val wrapper = response.awaitBody<ResourceWrapper<ModelResource>>()
                            SearchResult.Success(wrapper.content!!)
                        }
                        HttpStatus.NOT_FOUND -> SearchResult.NotFound
                        else -> {
                            val error = response.createExceptionAndAwait()
                            SearchResult.Error(error)
                        }
                    }
                }
        } catch (ex: Exception) {
            SearchResult.Error(ex)
        }
    }

}