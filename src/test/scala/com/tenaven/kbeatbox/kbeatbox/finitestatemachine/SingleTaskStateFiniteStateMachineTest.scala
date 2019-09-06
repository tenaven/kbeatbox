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
import com.tenaven.kbeatbox.kbeatbox.mock.{MockConcreteArmoringHeartBeatFiniteStateMachine, MockConcreteHeartBeatKeepingFiniteStateMachine, MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine, MockConcreteLiftedTaskStateFiniteStateMachine}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{LiftedTaskStateInterface, PriorityQueueTaskState}
import org.junit.Test

class SingleTaskStateFiniteStateMachineTest extends RunFiniteStateMachineTestBase {
  override val finiteStateMachines = List(
    new SingleTaskStateFiniteStateMachine(),
    new PriorityQueueTaskStateFiniteStateMachine(),
    new MockConcreteLiftedTaskStateFiniteStateMachine(),
    new MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine(),
    new MockConcreteHeartBeatKeepingFiniteStateMachine(),
    new MockConcreteArmoringHeartBeatFiniteStateMachine()
  )

  private val myname = "myname"

  @Test def verifyTaskStateWithMatching(): Unit = {
    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(AckEventCondition("A"))), AckEvent("A"))
    val expectTraces = Stream(List(), List(), List(StartedTask(myname)))

    runTestAndAssertTraces(s, expectTraces,
      specificFiniteStateMachines = finiteStateMachines.filter(fsm =>
        !fsm.isInstanceOf[ArmoringHeartBeatFiniteStateMachine])) // Armoring is failing this because AckEvent("A") is rejected.
  }


  @Test def verifyHeartbeat(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), HeartBeat(1L))

    val expectTraces = Stream(List(), List(), List(StartedTask(myname)))

    runTestAndAssertTraces(s, expectTraces)
  }


  @Test def verifyRemoveATaskThatwasPreviouslyScheduled(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), ScheduleTask(myname, Set()), HeartBeat(1L))

    val expectTraces = Stream(List(), List(), List(), List())

    runTestAndAssertTraces(s, expectTraces)
  }

  @Test def verifyRunTwice(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), HeartBeat(1L), HeartBeat(2L))

    val expectTraces = Stream(List(), List(), List(StartedTask(myname)), List())

    runTestAndAssertTraces(s, expectTraces)
  }

  @Test def verifyBeAbleToUpdateEvent(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), ScheduleTask(myname, Set(HeartBeatCondition(2L))), HeartBeat(1L), HeartBeat(2L))

    val expectTraces = Stream(List(), List(), List(), List(), List(StartedTask(myname)))

    runTestAndAssertTraces(s, expectTraces)
  }

  @Test def verifyDoesNotRunAScheduledTaskBeforeItReceivesHeartbeat(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(2L))), HeartBeat(1L))

    val expectTraces = Stream(List(), List(), List())

    runTestAndAssertTraces(s, expectTraces)
  }

  @Test def verifyRunEvenWhenAHeartbeatIsSkipped(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), HeartBeat(2L))

    val expectTraces = Stream(List(), List(), List(StartedTask(myname)))

    runTestAndAssertTraces(s, expectTraces)

  }

  @Test def verifyScheduleTaskInIdempotentWay(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), ScheduleTask(myname, Set(HeartBeatCondition(1L))),
      HeartBeat(2L))

    val expectTraces = Stream(List(), List(), List(), List(StartedTask(myname)))

    runTestAndAssertTraces(s, expectTraces)
  }


  @Test def verifyThatWeCanReceiveAckEvents(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), ScheduleTask(myname, Set(HeartBeatCondition(1L))),
      HeartBeat(1L), AckEvent(myname))

    val expectTraces = Stream(List(), List(), List(), List(StartedTask(myname)), List(FinishedTask(myname)))

    runTestAndAssertTraces(s, expectTraces, specificAssertOnFinalState= (state:Any, finiteStateMachine: MealyFiniteStateMachine[_,_,_]) => {
      state match {
        case typedState : LiftedTaskStateInterface => {
          val maybeState: Option[PriorityQueueTaskState] = typedState.getTaskForId(myname)
          assert(maybeState.isEmpty, s"$finiteStateMachine")
        }
        case _ => {}
      }
    })
  }

  @Test def verifyThatWeCanRescheduleATaskPreviouslyAcknowledged(): Unit = {
    val s: Stream[InputEvent] = Stream(ScheduleTask(myname, Set(HeartBeatCondition(1L))), HeartBeat(1L),
      AckEvent(myname),
      ScheduleTask(myname, Set(HeartBeatCondition(2L))))

    val expectTraces = Stream(List(), List(), List(StartedTask(myname)), List(FinishedTask(myname)), List())

    runTestAndAssertTraces(s, expectTraces)
  }

}
