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

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask, StartedTask}
import com.tenaven.kbeatbox.kbeatbox.mock.{MockConcreteArmoringHeartBeatFiniteStateMachine, MockConcreteHeartBeatKeepingFiniteStateMachine, MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine, MockConcreteLiftedTaskStateFiniteStateMachine}
import org.junit.Test

class PriorityQueueTaskStateFiniteStateMachineTest extends RunFiniteStateMachineTestBase {
  override val finiteStateMachines = List(
    new PriorityQueueTaskStateFiniteStateMachine(),
    new MockConcreteLiftedTaskStateFiniteStateMachine(),
    new MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine(),
    new MockConcreteHeartBeatKeepingFiniteStateMachine(),
    new MockConcreteArmoringHeartBeatFiniteStateMachine()
  )

  @Test def verifyTaskStateWithPriorityQueue(): Unit = {
    val s: Stream[InputEvent] = Stream(
      ScheduleTask("myname", Set(HeartBeatCondition(2L)), priority=1),
      ScheduleTask("myname", Set(HeartBeatCondition(1L)), priority=0),
      HeartBeat(1L),
      HeartBeat(2L))
    val expectTraces = Stream( List(), List(), List(), List(), List(StartedTask("myname")))

    runTestAndAssertTraces(s, expectTraces)
  }

  @Test def verifyTaskStateStackPop(): Unit = {
    val s: Stream[InputEvent] = Stream(
      ScheduleTask("myname", Set(HeartBeatCondition(2L)), priority=1),
      ScheduleTask("myname", Set(HeartBeatCondition(1L)), priority=0),
      ScheduleTask("myname", Set.empty, priority=1),
      HeartBeat(1L),
      HeartBeat(2L))
    val expectTraces = Stream( List(), List(), List(), List(), List(StartedTask("myname")), List())
    runTestAndAssertTraces(s, expectTraces)
  }

  @Test def verifyTaskStateStackPopWithConditionsAlreadyStarted(): Unit = {
    val s: Stream[InputEvent] = Stream(
      ScheduleTask("myname", Set(AckEventCondition("B"), AckEventCondition("C")), priority=1),
      ScheduleTask("myname", Set(AckEventCondition("A"), AckEventCondition("C")), priority=0),
      AckEvent("C"),
      ScheduleTask("myname", Set.empty, priority=1),
      AckEvent("A"))
    val expectTraces = Stream(List(), List(), List(), List(), List(), List(StartedTask("myname")))

    runTestAndAssertTraces(s, expectTraces,
      specificFiniteStateMachines = finiteStateMachines.filter(fsm =>
      !fsm.isInstanceOf[ArmoringHeartBeatFiniteStateMachine])) // Armoring is failing this because AckEvent("A") is rejected.
  }

  @Test def verifyTaskStateStackPopWithConditionsAlreadyStartedAndOverflowing(): Unit = {
    val s: Stream[InputEvent] = Stream(
      ScheduleTask("myname", Set(AckEventCondition("B"), AckEventCondition("C")), priority=1),
      ScheduleTask("myname", Set(AckEventCondition("A")), priority=0),
      AckEvent("C"),
      ScheduleTask("myname", Set.empty, priority=1),
      AckEvent("A"))
    val expectTraces = Stream(List(), List(), List(), List(), List(), List(StartedTask("myname")))

    runTestAndAssertTraces(s, expectTraces,
      specificFiniteStateMachines = finiteStateMachines.filter(fsm =>
        !fsm.isInstanceOf[ArmoringHeartBeatFiniteStateMachine])) // Armoring is failing this because AckEvent("A") is rejected.
  }
}
