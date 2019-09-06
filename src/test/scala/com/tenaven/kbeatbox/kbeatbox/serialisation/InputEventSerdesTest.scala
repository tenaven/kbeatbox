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

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, HeartBeat, HeartBeatCondition, InputEvent, InputEventDeserializer, InputEventSerializer, ScheduleTask}
import org.junit.Test

class InputEventSerdesTest extends SerdesTestBase[InputEvent] {
  override val serializer = new InputEventSerializer()
  override val deserializer = new InputEventDeserializer()

  @Test def verifyScheduleTaskIsSerializable() :Unit = {
    val taskName = "taskA"
    val initialEvent = ScheduleTask(taskName, Set())
    val clazzName = getClazzName(initialEvent.getClass)

    serializeAndDeserializeAndTest(initialEvent, List(taskName, clazzName))
  }

  @Test def verifyScheduleTaskIsSerializableWithCondition() :Unit = {
    val taskName = "taskA"
    val taskBName = "taskB"
    val initialEvent = ScheduleTask(taskName, Set(HeartBeatCondition(1L), AckEventCondition(taskBName)))

    val clazzName = getClazzName(initialEvent.getClass)

    val testStrings = List(taskName, taskBName, clazzName) ++
                      initialEvent.conditions.map(cond => getClazzName(cond.getClass)).toList

    serializeAndDeserializeAndTest(initialEvent, testStrings)
  }

  @Test def verifyHeartBeatIsSerializableWithCondition() :Unit = {

    val initialEvent = HeartBeat(2L)

    val clazzName = getClazzName(initialEvent.getClass)

    val testStrings = List(clazzName)

    serializeAndDeserializeAndTest(initialEvent, testStrings)
  }

  @Test def verifyAckEventIsSerializableWithCondition() :Unit = {

    val taskName = "Hello"
    val initialEvent = AckEvent(taskName)

    val clazzName = getClazzName(initialEvent.getClass)

    val testStrings = List(clazzName, taskName)

    serializeAndDeserializeAndTest(initialEvent, testStrings)
  }


}
