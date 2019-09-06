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
import net.deeppay.kmicrostreams.kbeatbox.mock.MockConcreteHeartBeatKeepingFiniteStateMachine
import net.deeppay.kmicrostreams.kbeatbox.stateapi._
import org.junit.Test




class HeartBeatKeepingFiniteStateMachineTest extends RunFiniteStateMachineTestBase {
  override val finiteStateMachines = List(new MockConcreteHeartBeatKeepingFiniteStateMachine())

  @Test def verifyLiftTasks(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("myname", Set(HeartBeatCondition(1L))), HeartBeat(1L))

    val expectedTraces = Stream(List(), List(), List(StartedTask("myname", Some(1L))))

    runTestAndAssertTraces(s, expectedTraces, currentTimeRemoved = false)
  }

  @Test def verifyRemoveTaskNameWhenTaskIsEmpty(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("myname", Set()))

    val expectedTraces = Stream(List(), List())

    runTestAndAssertTraces(s, expectedTraces, currentTimeRemoved = false)
  }
}


