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
package net.deeppay.kmicrostreams.kbeatbox.kafkaintegration

import com.typesafe.scalalogging.Logger
import net.deeppay.kmicrostreams.kbeatbox.api._
import net.deeppay.kmicrostreams.kbeatbox.stateapi._
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
