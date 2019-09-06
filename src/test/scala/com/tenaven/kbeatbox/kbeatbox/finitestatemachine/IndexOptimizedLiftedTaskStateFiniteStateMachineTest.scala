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
