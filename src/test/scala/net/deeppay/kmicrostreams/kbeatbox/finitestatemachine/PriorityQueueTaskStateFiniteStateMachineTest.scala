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
