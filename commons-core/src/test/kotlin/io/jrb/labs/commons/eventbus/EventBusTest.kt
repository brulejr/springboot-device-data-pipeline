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

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// Example event types
data class TestEvent(val value: Int) : Event
data class AnotherEvent(val message: String) : Event

@OptIn(ExperimentalCoroutinesApi::class)
class EventBusTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `subscriber receives published event`() = scope.runTest {
        val bus = EventBus<Event>(dispatcher = dispatcher)
        val handler = mockk<suspend (TestEvent) -> Unit>(relaxed = true)

        bus.subscribe(handler)

        bus.publish(TestEvent(42))
        advanceUntilIdle()

        coVerify(exactly = 1) { handler.invoke(TestEvent(42)) }
    }

    @Test
    fun `subscriber only receives matching event type`() = scope.runTest {
        val bus = EventBus<Event>(dispatcher = dispatcher)
        val handler = mockk<suspend (TestEvent) -> Unit>(relaxed = true)

        bus.subscribe(handler)

        bus.publish(AnotherEvent("ignored"))
        advanceUntilIdle()

        coVerify(exactly = 0) { handler.invoke(any()) }
    }

    @Test
    fun `multiple subscribers all receive the same event`() = scope.runTest {
        val bus = EventBus<Event>(dispatcher = dispatcher)
        val handler1 = mockk<suspend (TestEvent) -> Unit>(relaxed = true)
        val handler2 = mockk<suspend (TestEvent) -> Unit>(relaxed = true)

        bus.subscribe(handler1)
        bus.subscribe(handler2)

        bus.publish(TestEvent(1))
        advanceUntilIdle()

        coVerify(exactly = 1) { handler1.invoke(TestEvent(1)) }
        coVerify(exactly = 1) { handler2.invoke(TestEvent(1)) }
    }

    @Test
    fun `cancelled subscription does not receive further events`() = scope.runTest {
        val bus = EventBus<Event>(dispatcher = dispatcher)
        val handler = mockk<suspend (TestEvent) -> Unit>(relaxed = true)

        val sub = bus.subscribe(handler)

        bus.publish(TestEvent(1))
        advanceUntilIdle()
        sub.cancel()

        bus.publish(TestEvent(2))
        advanceUntilIdle()

        coVerify(exactly = 1) { handler.invoke(TestEvent(1)) }
        coVerify(exactly = 0) { handler.invoke(TestEvent(2)) }
    }

    @Test
    fun `send delivers event synchronously`() {
        val bus = EventBus<Event>(dispatcher = dispatcher)
        val handler = mockk<suspend (TestEvent) -> Unit>(relaxed = true)

        bus.subscribe(handler)

        bus.send(TestEvent(99))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { handler.invoke(TestEvent(99)) }
    }

    @Test
    fun `subscriber receives multiple events in order`() = scope.runTest {
        val bus = EventBus<Event>(dispatcher = dispatcher)
        val receivedEvents = mutableListOf<TestEvent>()

        val handler = mockk<suspend (TestEvent) -> Unit>()
        coEvery { handler.invoke(any()) } answers { receivedEvents.add(firstArg()) }

        bus.subscribe(handler)

        bus.publish(TestEvent(1))
        bus.publish(TestEvent(2))
        bus.publish(TestEvent(3))
        advanceUntilIdle()

        assertThat(receivedEvents)
            .hasSize(3)
            .extracting<Int> { it.value }
            .containsExactly(1, 2, 3)
    }

}
