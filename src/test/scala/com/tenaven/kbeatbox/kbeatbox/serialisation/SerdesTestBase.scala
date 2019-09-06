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
