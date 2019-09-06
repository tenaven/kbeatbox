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

import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.scalatest.junit.AssertionsForJUnit

trait SerdesTestBase[T] extends AssertionsForJUnit {
  val serializer : Serializer[T]
  val deserializer : Deserializer[T]

  def serialize(topic: String, data: T): Array[Byte] = {
    serializer.serialize("",data)
  }
  def deserialize(topic: String, data: Array[Byte]): T = {
    deserializer.deserialize("", data)
  }
  def getClazzName(clazz: Class[_]): String = {
    clazz.getTypeName.split(".", -1).last
  }
  def serializeAndDeserializeAndTest(initialEvent: T, testList: List[String]): Unit = {
    val bytes = serialize("StateTopic", initialEvent)
    val str = new String(bytes)
    println(str)
    testList.foreach(testName => {
      assert(str.contains(testName))
    })
    val resultingEvent: T = deserialize("StateTopic", bytes)
    assert(resultingEvent === initialEvent)
  }
}
