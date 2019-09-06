/*
 *  _
 * / |_ ___ _ __   __ ___   _____ _ __
 * | __/ _ \ '_ \ / _` \ \ / / _ \ '_ \
 * | ||  __/ | | | (_| |\ V /  __/ | | |
 * |_| \___|_| |_|\__,_| \_/ \___|_| |_|
 *
 *        Copyright 2019 TENAVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tenaven.kbeatbox.kbeatbox.finitestatemachine

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, HeartBeat, HeartBeatCondition, InputEvent, RejectedAckEvent, RejectedHeartBeat, RejectedTask, ScheduleTask, StartedTask}
import com.tenaven.kbeatbox.kbeatbox.mock.MockConcreteArmoringHeartBeatFiniteStateMachine
import org.junit.Test

class ArmoringHeartBeatFiniteStateMachineTest extends RunFiniteStateMachineTestBase {
  override val finiteStateMachines = List(
    new MockConcreteArmoringHeartBeatFiniteStateMachine()
  )

  @Test def verifyRejectHeartBeatWhenEarlierThanMostRecentlyReceiveHeartBeat(): Unit = {
    val s: Stream[InputEvent] = {
      Stream(HeartBeat(10L), HeartBeat(5L))
    }
    val expectTraces = Stream(List(), List(), List(RejectedHeartBeat("[HeartBeat(5)] rejected as it is earlier than most recently received heartbeat [10]", Some(10L))))
    runTestAndAssertTraces(s, expectTraces, currentTimeRemoved=false)
  }

  @Test def verifyRejectScheduleTaskEarlierThanPresent(): Unit = {
    val s: Stream[InputEvent] = Stream(HeartBeat(10L), ScheduleTask("A", Set(HeartBeatCondition(5L))))
    val expectTraces = Stream(List(), List(), List(RejectedTask("[ScheduleTask(A,Set(HeartBeatCondition(5)),0)] rejected as it is scheduled earlier than most recently received heartbeat [10]", "A", Some(10L))))
    runTestAndAssertTraces(s, expectTraces, currentTimeRemoved=false)
  }

  @Test def verifyRejectScheduleTaskAlreadyTriggered(): Unit = {
    val s: Stream[InputEvent] = Stream(HeartBeat(1L),
      ScheduleTask("A", Set(HeartBeatCondition(2L))),
      HeartBeat(2L),
      ScheduleTask("A", Set(HeartBeatCondition(5L))))
    val expectTraces = Stream(List(), List(), List(), List(StartedTask("A", Some(2))), List(RejectedTask("[ScheduleTask(A,Set(HeartBeatCondition(5)),0)] rejected as [A] is already triggered", "A", Some(2L))))
    runTestAndAssertTraces(s, expectTraces, currentTimeRemoved=false)
  }

  @Test def verifyRejectAckEventOfUnExistingScheduleTask(): Unit = {
    val s: Stream[InputEvent] = Stream(AckEvent("A"))
    val expectTraces = Stream(List(), List(RejectedAckEvent("[AckEvent(A)] rejected as [A] is not yet scheduled (thus not triggered)", "A", None)))
    runTestAndAssertTraces(s, expectTraces, currentTimeRemoved=false)
  }

  @Test def verifyRejectAckEventNotTriggered(): Unit = {
    val s: Stream[InputEvent] = Stream(ScheduleTask("A", Set(HeartBeatCondition(5L))), AckEvent("A"))
    val expectTraces = Stream(List(), List(), List(RejectedAckEvent("[AckEvent(A)] rejected as [A] is not yet triggered", "A", None)))
    runTestAndAssertTraces(s, expectTraces, currentTimeRemoved=false)
  }

  @Test def verifyRejectTaskWithMultipleHeartbeat(): Unit = {
    val s: Stream[InputEvent] = {
      Stream(HeartBeat(1L), ScheduleTask("A", Set(HeartBeatCondition(1L), HeartBeatCondition(2L))))
    }
    val expectTraces = Stream(List(), List(), List(RejectedTask("[ScheduleTask(A,Set(HeartBeatCondition(1), HeartBeatCondition(2)),0)] rejected as it contains multiple heartbeats","A",Some(1))))
    runTestAndAssertTraces(s, expectTraces, currentTimeRemoved=false)
  }

}

