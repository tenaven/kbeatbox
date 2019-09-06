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
package com.tenaven.kbeatbox.kbeatbox.serialisation

import com.tenaven.kbeatbox.kbeatbox.api.{AckEventCondition, HeartBeatCondition, ScheduleTask}
import com.tenaven.kbeatbox.kbeatbox.finitestatemachine.PriorityQueueTaskStateFiniteStateMachine
import com.tenaven.kbeatbox.kbeatbox.stateapi.{PriorityQueueTaskState, PriorityQueueTaskStateDeserializer, PriorityQueueTaskStateSerializer}
import org.junit.Test

class PriorityQueueTaskStateSerdesTest extends SerdesTestBase[PriorityQueueTaskState] {
  override val serializer = new PriorityQueueTaskStateSerializer()
  override val deserializer = new PriorityQueueTaskStateDeserializer

  @Test def verifyPriorityQueueTaskStateTaskIsSerializable() :Unit = {
    val taskName = "taskA"
    val initialEvent = ScheduleTask(taskName, Set(AckEventCondition("A"), HeartBeatCondition(2L)))
    val finiteStateMachine = new PriorityQueueTaskStateFiniteStateMachine()
    val state = finiteStateMachine.transition(finiteStateMachine.zero(), initialEvent)._1
    val clazzName = getClazzName(initialEvent.getClass)

    serializeAndDeserializeAndTest(state, List(taskName, clazzName))
  }

}
