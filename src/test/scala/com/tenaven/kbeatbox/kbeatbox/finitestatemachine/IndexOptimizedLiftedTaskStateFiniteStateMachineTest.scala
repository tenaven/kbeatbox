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
import com.tenaven.kbeatbox.kbeatbox.mock.{MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine, MockConcreteLiftedTaskStateFiniteStateMachine}
import com.tenaven.kbeatbox.kbeatbox.stateapi.IndexOptimizedLiftedTaskStateInterface
import org.junit.Test


// Interesting : https://github.com/fpinscala/fpinscala/wiki/Chapter-6:-Purely-functional-state

class IndexOptimizedLiftedTaskStateFiniteStateMachineTest extends RunFiniteStateMachineTestBase {

  override val finiteStateMachines = List(new MockConcreteLiftedTaskStateFiniteStateMachine(), new MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine())

  @Test def verifyIndexOptimizedLiftTasksWithMultipleTasks(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("task1", Set(AckEventCondition("A"))),
      ScheduleTask("task2", Set(AckEventCondition("A"), AckEventCondition("B"))),
      AckEvent("A"),
      AckEvent("B")
    )

    val expectedTraces = Stream(List(), List(), List(), List(StartedTask("task1")),
      List(StartedTask("task2")))

    runTestAndAssertTraces(s, expectedTraces, specificAssertOnFinalState = {
      case (indexState: IndexOptimizedLiftedTaskStateInterface, fsm) => assert(indexState.getTaskNamesForFactFromIndex(AckEvent("A")) == Set("A"), s"$fsm")
      case _ =>
    })
  }

  @Test def verifyIndexOptimizedLiftTasksWithHeartBeat(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("task1", Set(HeartBeatCondition(1L))),
      HeartBeat(1L)
    )

    val expectedTraces = Stream(List(), List(), List(StartedTask("task1")))

    runTestAndAssertTraces(s, expectedTraces, specificAssertOnFinalState = {
      case (indexState: IndexOptimizedLiftedTaskStateInterface, fsm) => assert(indexState.getTaskNamesForFactFromIndex(HeartBeat(5)).isEmpty, s"$fsm")
      case _ =>
    })
  }

  @Test def verifyIndexOptimizedLiftTasksIsIdempotent(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("task1", Set(AckEventCondition("A"))),
      ScheduleTask("task1", Set(AckEventCondition("A"))),
      AckEvent("A"),
      AckEvent("A")
    )

    val expectedTraces = Stream(List(), List(), List(), List(StartedTask("task1",None)), List())

    runTestAndAssertTraces(s, expectedTraces, specificAssertOnFinalState = {
      case (indexState: IndexOptimizedLiftedTaskStateInterface, fsm) => assert(indexState.getTaskNamesForFactFromIndex(AckEvent("A")) == Set("A"), s"$fsm")
      case _ =>
    })
  }
}
