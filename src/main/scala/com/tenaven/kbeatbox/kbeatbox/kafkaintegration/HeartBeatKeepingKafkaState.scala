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
package com.tenaven.kbeatbox.kbeatbox.kafkaintegration

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, Condition, Fact, HeartBeat, HeartBeatCondition}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{HeartBeatKeepingStateInterface, PriorityQueueTaskState}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.streams.state.{KeyValueIterator, KeyValueStore}

import scala.collection.JavaConverters

/**
  * this HeartBeatKeepingStateInterface provides Kafka-integrated state for a
  * HeartBeatKeepingFiniteStateMachine.
  */
class HeartBeatKeepingKafkaState extends HeartBeatKeepingStateInterface {
  var heartBeatStateStore : KeyValueStore[String, Long] = _
  var ackIndex : KeyValueStore[String, Set[String]] = _
  var hbIndex : KeyValueStore[Long, Set[String]] = _
  var taskStates : KeyValueStore[String, PriorityQueueTaskState] = _
  val minimumHeartBeat = 0

  val logger = Logger(classOf[HeartBeatKeepingKafkaState])



  override def update(currentTime: Long) : this.type = {
    logger.debug(s"lastReceivedHeartBeat = [${currentTime}]")
    heartBeatStateStore.put(KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant, currentTime)
    this
  }

  override def getLastReceivedHeartBeatTime() : Option[Long] = {
    val maybeLastReceivedHeartBeat = Option(heartBeatStateStore.get(KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant))
    logger.debug(s"getLastReceivedHeartBeat = [${maybeLastReceivedHeartBeat}]")
    maybeLastReceivedHeartBeat
  }

  override def getTaskNamesForFactFromIndex(fact: Fact) : Set[String] = {
    logger.debug(s"getTaskNamesForFactFromIndex(${fact}")
    fact match {
      case AckEvent(ackId) => {
        Option(ackIndex.get(ackId)).map(_.toSet).getOrElse(Set.empty)
      }
      case HeartBeat(fromTime) => {
        var range: KeyValueIterator[Long, Set[String]] = null
        try {
          range = hbIndex.range(minimumHeartBeat, fromTime)

          val result: Set[String] = JavaConverters.asScalaIterator(range).flatMap( _.value ).toSet

          logger.debug(s"getTaskNamesForFactFromIndex(${fact}) finished - result = [$result]")
          result
        } finally {
          if (range != null) {
            range.close()
          }
        }
      }
    }
  }

  override def updateAListOfTasks(updatedTasks: List[(String, Option[PriorityQueueTaskState])]): this.type = {
    updatedTasks.foreach(tuple => {
      val (taskName, maybeTask)  = tuple

      maybeTask match {
        case Some(task) => {
          logger.debug(s"updateAListOfTasks [$taskName] -> [$task]")
          taskStates.put(taskName, task)
        }
        case None => {
          logger.debug(s"updateAListOfTasks remove task : [$taskName]")
          taskStates.delete(taskName)
        }
      }

    })
    this
  }

  override def applyFunctionOnTaskConditionsInIndex(taskName: String, condition: Condition, function: (Set[String], String) => Set[String]) : this.type = {
    logger.debug(s"updating Index for condition [$condition] and taskName ${taskName}")
    condition match {
      case HeartBeatCondition(fromTime) => {
        val taskNameList = function.apply(Option(hbIndex.get(fromTime)).getOrElse(Set.empty), taskName)
        if (taskNameList.isEmpty) {
          hbIndex.delete(fromTime)
        } else {
          hbIndex.put(fromTime, taskNameList)
        }
      }
      case AckEventCondition(ackEvent) => {
        val taskNameList = function.apply(Option(ackIndex.get(ackEvent)).getOrElse(Set.empty), taskName)
        if (taskNameList.isEmpty) {
          ackIndex.delete(ackEvent)
        } else {
          ackIndex.put(ackEvent, taskNameList)
        }
      }
    }
    this
  }

  override def getTaskForId(taskId: String): Option[PriorityQueueTaskState] = {
    Option(taskStates.get(taskId))
  }
}
