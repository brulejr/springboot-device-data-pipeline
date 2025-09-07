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

import io.jrb.labs.commons.eventbus.SystemEvent
import io.jrb.labs.commons.eventbus.SystemEvent.StartService
import io.jrb.labs.commons.eventbus.SystemEvent.StopService
import io.jrb.labs.commons.eventbus.SystemEvent.SystemMessage
import io.jrb.labs.commons.eventbus.SystemEventBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class ControllableServiceTest {

    private lateinit var systemEventBus: SystemEventBus
    private lateinit var service: TestService
    private val messages = mutableListOf<SystemMessage>()

    @BeforeEach
    fun setup() {
        systemEventBus = SystemEventBus()
        service = TestService("test-service", systemEventBus)

        messages.clear()

        // capture system messages
        systemEventBus.subscribe<SystemMessage> {
            messages.add(it)
        }
    }

    @Test
    fun `startup should subscribe and start service`() {
        service.start()

        assertThat(service.isRunning()).isTrue()
        assertThat(service.startedCount).isEqualTo(1)
        assertThat(messages.map { it.code }).containsExactly("service.starting", "service.started")
    }

    @Test
    fun `shutdown should stop and cancel subscription`() {
        service.start()
        service.shutdown()

        assertThat(service.isRunning()).isFalse()
        assertThat(service.stoppedCount).isEqualTo(1)
    }

    @ParameterizedTest(name = "{index} => event={0}, running={1}, starts={2}, stops={3}, codes={4}")
    @MethodSource("scenarios")
    fun `should handle service events correctly`(scenario: TestScenario) {
        println("Running scenario: $scenario")

        // setup
        val latch = CountDownLatch(scenario.expectedCodes.size)
        systemEventBus.subscribe<SystemMessage> {
            if (it.serviceName == service.serviceName) {
                println("Received message: $it")
                latch.countDown()
            }
        }

        // always start baseline
        service.start()

        // given
        scenario.setup(service)

        // when
        systemEventBus.send(scenario.event)

        // then
        println("Waiting for ${scenario.expectedCodes.size} messages")
        assertThat(latch.await(5, TimeUnit.SECONDS))
            .withFailMessage("Did not receive expected messages in time")
            .isTrue()

        assertThat(service.isRunning()).isEqualTo(scenario.expectedRunning)
        assertThat(service.startedCount).isEqualTo(scenario.expectedStarts)
        assertThat(service.stoppedCount).isEqualTo(scenario.expectedStops)
        assertThat(messages.map { it.code }).containsExactlyElementsOf(scenario.expectedCodes)

        service.shutdown()
    }

    companion object {
        @JvmStatic
        fun scenarios(): Stream<TestScenario> = Stream.of(
            TestScenario(
                name = "service startup",
                setup = { service -> service.stop() },
                event = StartService("test-service"),
                expectedRunning = true, // expected running
                expectedStarts = 2,    // started once on startup, once on event
                expectedStops = 1,    // stopped once before event
                expectedCodes = listOf(
                    "service.starting", "service.started",
                    "service.stopping", "service.stopped",
                    "service.starting", "service.started"
                )
            ),
            TestScenario(
                name = "service shutdown",
                event = StopService("test-service"),
                expectedRunning = false, // expected running
                expectedStarts = 1,    // started once on startup, once on event
                expectedStops = 1,    // stopped once before event
                expectedCodes = listOf(
                    "service.starting", "service.started",
                    "service.stopping", "service.stopped"
                )
            ),
            TestScenario(
                name = "start service already running",
                event = StartService("test-service"),
                expectedRunning = true, // expected running
                expectedStarts = 1,    // started once on startup, once on event
                expectedStops = 0,    // stopped once before event
                expectedCodes = listOf(
                    "service.starting", "service.started"
                )
            ),
            TestScenario(
                name = "stop service already stopped",
                setup = { service -> service.stop() },
                event = StopService("test-service"),
                expectedRunning = false, // expected running
                expectedStarts = 1,    // started once on startup, once on event
                expectedStops = 1,    // stopped once before event
                expectedCodes = listOf(
                    "service.starting", "service.started",
                    "service.stopping", "service.stopped"
                )
            ),
            TestScenario(
                name = "other service event",
                event = StartService("other-service"),
                expectedRunning = true, // expected running
                expectedStarts = 1,    // started once on startup, once on event
                expectedStops = 0,    // stopped once before event
                expectedCodes = listOf(
                    "service.starting", "service.started"
                )
            )
        )
    }

    data class TestScenario(
        val name: String,
        val setup: (service: TestService) -> Unit = {_ -> },
        val event: SystemEvent,
        val expectedRunning: Boolean,
        val expectedStarts: Int,
        val expectedStops: Int,
        val expectedCodes: List<String>
    )

    // --- test helper subclass ---
    class TestService(
        override val serviceName: String,
        systemEventBus: SystemEventBus
    ) : ControllableService(systemEventBus) {

        var startedCount = 0
        var stoppedCount = 0

        override fun onStart() {
            startedCount++
            println("*** startedCount = $startedCount")
        }

        override fun onStop() {
            stoppedCount++
            println("*** stoppedCount = $stoppedCount")
        }
    }

}
