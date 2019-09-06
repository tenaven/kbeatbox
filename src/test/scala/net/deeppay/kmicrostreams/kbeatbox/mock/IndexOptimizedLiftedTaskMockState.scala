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
package net.deeppay.kmicrostreams.kbeatbox.mock

import net.deeppay.kmicrostreams.kbeatbox.api._
import net.deeppay.kmicrostreams.kbeatbox.stateapi._

import scala.collection.immutable.TreeMap

/**
  * This mock test allows testing of the IndexOptimizedLiftedTaskStateFiniteStateMachine
  */
class IndexOptimizedLiftedTaskMockState(val ackIndex :  Map[AckEventCondition, Set[String]] = Map.empty,
                                        val hbIndex: TreeMap[HeartBeatCondition, Set[String]] = TreeMap.empty,
                                        override val taskStates : Map[String, PriorityQueueTaskState])
            extends LiftedTaskMockState(taskStates)
            with IndexOptimizedLiftedTaskStateInterface {

  override def getTaskNamesForFactFromIndex(fact : Fact): Set[String] = {
    val sourceState = this
    fact match {
      case HeartBeat(fromTime) => {
        val cond = HeartBeatCondition(fromTime)
        sourceState.hbIndex.to(cond).flatMap(_._2).toSet
      }
      case AckEvent(eventId) => {
        val cond = AckEventCondition(eventId)
        Set(eventId) ++ sourceState.ackIndex.getOrElse(cond, Set.empty)  // Add the eventId to the Set is important to allow for FinishedTask trace to appear.
      }
    }
  }

  def applyFunctionOnTaskConditionsInIndex(taskName: String, condition: Condition, function: (Set[String], String) => Set[String]): this.type = {
    val sourceState = this
    condition match {
      case cond: HeartBeatCondition => {
        val taskNameList = function.apply(sourceState.hbIndex.getOrElse(cond, Set.empty), taskName)
        val newIndex = if (taskNameList.isEmpty) {
          sourceState.hbIndex - cond
        } else {
          sourceState.hbIndex.updated(cond, taskNameList)
        }
        updateHbIndex(sourceState.asInstanceOf[this.type], newIndex)
      }
      case cond: AckEventCondition => {
        val taskNameList = function.apply(sourceState.ackIndex.getOrElse(cond, Set.empty), taskName)
        val newIndex: Map[AckEventCondition, Set[String]] = if (taskNameList.isEmpty){
          sourceState.ackIndex - cond
        } else {
          sourceState.ackIndex.updated(cond, taskNameList)
        }
        updateAckIndex(sourceState.asInstanceOf[this.type], newIndex)
      }
    }
  }

  def updateHbIndex(sourceState : this.type, hbIndex : TreeMap[HeartBeatCondition, Set[String]]) : this.type = {
    new IndexOptimizedLiftedTaskMockState(ackIndex = sourceState.ackIndex,
      hbIndex = hbIndex,
      taskStates = sourceState.taskStates).asInstanceOf[this.type]
  }

  def updateAckIndex(sourceState : this.type, ackIndex : Map[AckEventCondition, Set[String]]) : this.type = {
    new IndexOptimizedLiftedTaskMockState(ackIndex = ackIndex,
      hbIndex = sourceState.hbIndex,
      taskStates = sourceState.taskStates).asInstanceOf[this.type]
  }

  override def updateStateWithTasks(sourceState: this.type, updatedTasks: Map[String, PriorityQueueTaskState]): this.type = {
    new IndexOptimizedLiftedTaskMockState(ackIndex = sourceState.ackIndex,
      hbIndex = sourceState.hbIndex,
      taskStates = updatedTasks).asInstanceOf[this.type]
  }

  override def hashCode(): Int = {
    this.ackIndex.hashCode() ^ this.hbIndex.hashCode() ^ super.hashCode()
  }

  override def equals(obj: scala.Any): Boolean = super.equals(obj)
}
