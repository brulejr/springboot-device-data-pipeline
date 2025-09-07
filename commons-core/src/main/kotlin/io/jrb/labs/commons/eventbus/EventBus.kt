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
package io.jrb.labs.commons.eventbus

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.jvm.java

open class EventBus<E : Event>(
    eventBusCapacity: Int = 100,
    dispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
) {
    private val _scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _events = MutableSharedFlow<E>(extraBufferCapacity = eventBusCapacity)

    suspend fun publish(event: E) {
        _events.emit(event)
    }

    fun send(event: E) = runBlocking { publish(event) }

    fun <T : E> subscribe(clazz: Class<T>, handler: suspend (T) -> Unit): Subscription {
        val job = _scope.launch(start = CoroutineStart.UNDISPATCHED) { // important!
            _events
                .filter { clazz.isInstance(it) }
                .map { clazz.cast(it)!! }
                .collect { handler(it) }
        }
        return Subscription(job)
    }

    class Subscription(private val job: kotlinx.coroutines.Job) {
        fun cancel() = job.cancel()
    }

    inline fun <reified T : E> subscribe(noinline handler: suspend (T) -> Unit): Subscription =
        subscribe(T::class.java, handler)

}