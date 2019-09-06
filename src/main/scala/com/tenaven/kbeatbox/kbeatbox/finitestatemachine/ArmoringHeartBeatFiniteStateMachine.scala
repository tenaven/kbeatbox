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
package com.tenaven.kbeatbox.kbeatbox.finitestatemachine

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, Condition, Fact, HeartBeat, HeartBeatCondition, RejectedAckEvent, RejectedHeartBeat, RejectedTask, ScheduleTask, Trace}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{HeartBeatKeepingStateInterface, PriorityQueueTaskState}
import com.typesafe.scalalogging.Logger



/**
  * Responsible for protecting the underlying FiniteStateMachine from wrong input.
  * It rejects InputEvents.
  */
abstract class ArmoringHeartBeatFiniteStateMachine extends HeartBeatKeepingFiniteStateMachine {
  override val logger = Logger(classOf[ArmoringHeartBeatFiniteStateMachine])

  override def updateTask(sourceState: HeartBeatKeepingStateInterface, task: ScheduleTask): (HeartBeatKeepingStateInterface, List[Trace]) = {
    val maybeMostRecentlyReceivedHeartBeat: Option[Long] = sourceState.getLastReceivedHeartBeatTime()
    (task, maybeMostRecentlyReceivedHeartBeat) match {

      case (ScheduleTask(_, conditions, _), _) if
            (conditions count ((cond: Condition) => cond.isInstanceOf[HeartBeatCondition])) > 1 => {

        reject(sourceState, task.taskId, maybeMostRecentlyReceivedHeartBeat,
          message = s"[$task] rejected as it contains multiple heartbeats", traceConstructor = RejectedTask(_, _, _))
      }

      case (ScheduleTask(_, conditions, _), Some(mostRecentlyReceivedHeartBeat)) if
        conditions.collectFirst { case hbCond: HeartBeatCondition => hbCond }.exists(
                                              _.fromTime <= mostRecentlyReceivedHeartBeat)
        => {

        reject(sourceState, task.taskId, maybeMostRecentlyReceivedHeartBeat,
          message = s"[$task] rejected as it is scheduled earlier than most recently received heartbeat [${mostRecentlyReceivedHeartBeat}]", traceConstructor = RejectedTask(_, _, _))
      }

      case (ScheduleTask(taskId, _, _), _) => {
        val maybeSingleTaskState: Option[PriorityQueueTaskState] = sourceState.getTaskForId(taskId)
        maybeSingleTaskState match {
          case Some(PriorityQueueTaskState(savedTaskName, _, _, isTriggered)) if isTriggered => {
            reject(sourceState, task.taskId, maybeMostRecentlyReceivedHeartBeat,
              message = s"[$task] rejected as [$savedTaskName] is already triggered", traceConstructor = RejectedTask(_, _, _))
          }
          case _ => super.updateTask(sourceState, task)
        }
      }

      case _ => super.updateTask(sourceState, task)
    }

  }

  private def reject(sourceState: HeartBeatKeepingStateInterface, inputEvent: String, maybeMostRecentlyReceivedHeartBeat: Option[Long], message: String, traceConstructor: (String, String, Option[Long]) => Trace): (HeartBeatKeepingStateInterface, List[Trace]) = {
    logger.warn(message)
    (sourceState, List(traceConstructor.apply(message, inputEvent, maybeMostRecentlyReceivedHeartBeat)))
  }

  override def acceptFact(sourceState: HeartBeatKeepingStateInterface, fact: Fact): (HeartBeatKeepingStateInterface, List[Trace]) = {
    val maybeMostRecentlyReceivedHeartBeat = sourceState.getLastReceivedHeartBeatTime()

    (fact, maybeMostRecentlyReceivedHeartBeat) match {
      case (HeartBeat(newHeartBeatTimestamp), Some(mostRecentlyReceivedHeartBeat)) if mostRecentlyReceivedHeartBeat >= newHeartBeatTimestamp => {
        reject(sourceState, newHeartBeatTimestamp.toString, maybeMostRecentlyReceivedHeartBeat,
          message = s"[$fact] rejected as it is earlier than most recently received heartbeat [${maybeMostRecentlyReceivedHeartBeat.get}]", traceConstructor = (x, y, z) => RejectedHeartBeat(x, z))
      }

      case (AckEvent(taskId), _) => {

        sourceState.getTaskForId(taskId) match {
          case None => {
            reject(sourceState, taskId, maybeMostRecentlyReceivedHeartBeat,
              message = s"[$fact] rejected as [$taskId] is not yet scheduled (thus not triggered)", traceConstructor = RejectedAckEvent(_, _, _))
          }

          case Some(PriorityQueueTaskState(_, _, _, isTriggered)) if !isTriggered => {
            reject(sourceState, taskId, maybeMostRecentlyReceivedHeartBeat,
              message = s"[$fact] rejected as [$taskId] is not yet triggered", traceConstructor = RejectedAckEvent(_, _, _))
          }
          case _ => super.acceptFact(sourceState, fact)
        }
      }
      case _ => super.acceptFact(sourceState, fact)
    }
  }
}
