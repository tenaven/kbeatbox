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
