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
package io.jrb.labs.commons.service

import io.jrb.labs.commons.client.ResourceWrapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class CrudResponse {

    companion object {

        suspend fun <T> crudResponse(
            actionFn: suspend () -> CrudOutcome<T>,
            successFn: (CrudOutcome.Success<T>) -> ResponseEntity<ResourceWrapper<T>> = { defaultSuccessFn(it) },
            notFoundFn: (CrudOutcome.NotFound) -> ResponseEntity<ResourceWrapper<T>> = { defaultNotFoundFn(it) },
            conflictFn: (CrudOutcome.Conflict) -> ResponseEntity<ResourceWrapper<T>> = { defaultConflictFn(it) },
            invalidFn: (CrudOutcome.Invalid) -> ResponseEntity<ResourceWrapper<T>> = { defaultInvalidFn(it) },
            errorFn: (CrudOutcome.Error) -> ResponseEntity<ResourceWrapper<T>> = { defaultErrorFn(it) }
        ): ResponseEntity<ResourceWrapper<T>> {
            return when (val result = actionFn()) {
                is CrudOutcome.Success -> successFn(result)
                is CrudOutcome.NotFound -> notFoundFn(result)
                is CrudOutcome.Conflict -> conflictFn(result)
                is CrudOutcome.Invalid -> invalidFn(result)
                is CrudOutcome.Error -> errorFn(result)
            }

        }

        private fun <T> defaultSuccessFn(result: CrudOutcome.Success<T>): ResponseEntity<ResourceWrapper<T>> {
            return ResponseEntity.ok(ResourceWrapper(content = result.data))
        }

        private fun <T> defaultNotFoundFn(result: CrudOutcome.NotFound): ResponseEntity<ResourceWrapper<T>> {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResourceWrapper(
                    status = HttpStatus.NOT_FOUND.value(),
                    messages = listOf("Unable to find resource: ${result.id}")
                ))
        }

        private fun <T> defaultConflictFn(result: CrudOutcome.Conflict): ResponseEntity<ResourceWrapper<T>> {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResourceWrapper(
                    status = HttpStatus.CONFLICT.value(),
                    messages = listOf(result.reason)
                ))
        }

        private fun <T> defaultInvalidFn(result: CrudOutcome.Invalid): ResponseEntity<ResourceWrapper<T>> {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResourceWrapper(
                    status = HttpStatus.BAD_REQUEST.value(),
                    messages = listOf(result.reason)
                ))
        }

        private fun <T> defaultErrorFn(result: CrudOutcome.Error): ResponseEntity<ResourceWrapper<T>> {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResourceWrapper(
                    status = HttpStatus.BAD_REQUEST.value(),
                    messages = listOf(result.message)
                ))
        }

    }

}