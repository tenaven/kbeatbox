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
