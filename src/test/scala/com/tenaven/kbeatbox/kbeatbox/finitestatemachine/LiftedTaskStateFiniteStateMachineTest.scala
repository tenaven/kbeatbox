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

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, FinishedTask, HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask, StartedTask}
import com.tenaven.kbeatbox.kbeatbox.mock.{MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine, MockConcreteLiftedTaskStateFiniteStateMachine}
import com.tenaven.kbeatbox.kbeatbox.stateapi.LiftedTaskStateInterface
import org.junit.Test

class LiftedTaskStateFiniteStateMachineTest extends RunFiniteStateMachineTestBase {

  override val finiteStateMachines = List(new MockConcreteLiftedTaskStateFiniteStateMachine(), new MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine())

  @Test def verifyLiftTasks(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("myname", Set(AckEventCondition("A"))), AckEvent("A"))

    val expectedTraces = Stream(List(), List(), List(StartedTask("myname")))

    runTestAndAssertTraces(s, expectedTraces)
  }

  @Test def verifyLiftTasksWithMultipleTasks(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("task1", Set(AckEventCondition("A"))),
      ScheduleTask("task2", Set(AckEventCondition("A"), AckEventCondition("B"))),
      AckEvent("A"),
      AckEvent("B")
    )

    val expectedTraces = Stream(List(), List(), List(), List(StartedTask("task1")),
      List(StartedTask("task2")))

    runTestAndAssertTraces(s, expectedTraces)
  }


  @Test def verifyLiftTasksWithChaining(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("A", Set(HeartBeatCondition(1L))),
      ScheduleTask("B", Set(HeartBeatCondition(2L))),
      ScheduleTask("C", Set(AckEventCondition("A"), AckEventCondition("B"))),
      HeartBeat(1L), HeartBeat(2L),
      AckEvent("A"), AckEvent("B"),
    )

    val expectedTraces = Stream(List(), List(), List(), List(), List(StartedTask("A")),
      List(StartedTask("B")),
      List(FinishedTask("A")),
      List(FinishedTask("B"), StartedTask("C"))
    )

    runTestAndAssertTraces(s, expectedTraces)
  }

  @Test def verifyLiftTasksWithPriorityTaskRemoved(): Unit = {
    val myname = "myname"
    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), ScheduleTask(myname, Set()))

    val expectedTraces = Stream(List(), List(), List())

    runTestAndAssertTraces(s, expectedTraces, specificAssertOnFinalState = {
      case (newTaskState: LiftedTaskStateInterface, fsm) => assert(newTaskState.getTaskForId(myname).isEmpty , s"$fsm")
      case _ =>
    })
  }
}
