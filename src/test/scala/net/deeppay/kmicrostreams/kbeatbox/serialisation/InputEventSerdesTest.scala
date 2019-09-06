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
package net.deeppay.kmicrostreams.kbeatbox.serialisation

import net.deeppay.kmicrostreams.kbeatbox._
import net.deeppay.kmicrostreams.kbeatbox.api._
import net.deeppay.kmicrostreams.kbeatbox.finitestatemachine._
import net.deeppay.kmicrostreams.kbeatbox.stateapi._
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
