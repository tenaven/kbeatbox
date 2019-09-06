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
import net.deeppay.kmicrostreams.kbeatbox.mock.MockConcreteArmoringHeartBeatFiniteStateMachine
import net.deeppay.kmicrostreams.kbeatbox.stateapi._
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

