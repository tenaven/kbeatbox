/*
 *      _                 ____               ____    _    ____
 *   __| | ___  ___ _ __ |  _ \ __ _ _   _  / ___|  / \  / ___|
 *  / _` |/ _ \/ _ \ '_ \| |_) / _` | | | | \___ \ / _ \ \___ \
 * | (_| |  __/  __/ |_) |  __/ (_| | |_| |  ___) / ___ \ ___) |
 *  \__,_|\___|\___| .__/|_|   \__,_|\__, | |____/_/   \_\____/
 *                 |_|               |___/
 *
 * (c) 2018 deepPay SAS
 * All rights reserved
 *
 * This software is made available to third parties by deepPay free of charge and subject to the following license and conditions :
 * (i) it is provided independently of any existing contract(s) or relationships;
 * (ii) it is provided as-is and for educational purposes only;
 * (iii) it is provided without any warranty or support;
 * (iv) deepPay accepts no liability in relation to the use of this software whatsoever; and
 * (v) this license shall continue until terminated, in deepPay sole discretion, on notice to the user
 */
package net.deeppay.kmicrostreams.kbeatbox.finitestatemachine

import net.deeppay.kmicrostreams.kbeatbox.api._
import net.deeppay.kmicrostreams.kbeatbox.mock.{MockConcreteArmoringHeartBeatFiniteStateMachine, MockConcreteHeartBeatKeepingFiniteStateMachine, MockConcreteIndexOptimizedLiftedTaskStateFiniteStateMachine, MockConcreteLiftedTaskStateFiniteStateMachine}
import net.deeppay.kmicrostreams.kbeatbox.stateapi._
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
