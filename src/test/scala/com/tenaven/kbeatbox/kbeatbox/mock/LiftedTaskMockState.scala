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
package com.tenaven.kbeatbox.kbeatbox.mock

import com.tenaven.kbeatbox.kbeatbox.api.Fact
import com.tenaven.kbeatbox.kbeatbox.stateapi.{LiftedTaskStateInterface, PriorityQueueTaskState}


/**
  * This mock state for the LiftedTaskStateFiniteStateMachine allows testing of the logic in the aforementioned
  * FiniteStateMachine.
  */
case class LiftedTaskMockState(taskStates : Map[String, PriorityQueueTaskState])
    extends LiftedTaskStateInterface {


  def updateAListOfTasks(updatedTasks: List[(String, Option[PriorityQueueTaskState])]): this.type = {
    val sourceState = this
    val (taskToUpdate, removedTasks) = updatedTasks.partition(_._2.isDefined)
    val updatingTuples = taskToUpdate.map(tuple => (tuple._1, tuple._2.get))
    val removedTaskNames = removedTasks.map(_._1)
    val newTaskStates: Map[String, PriorityQueueTaskState] = (sourceState.taskStates -- removedTaskNames) ++ updatingTuples
    updateStateWithTasks(sourceState.asInstanceOf[this.type], newTaskStates)
  }

  def updateStateWithTasks(sourceState: this.type, updatedTasks: Map[String, PriorityQueueTaskState]): this.type = {
    sourceState.copy(taskStates = updatedTasks).asInstanceOf[this.type]
  }


  override def getTaskForId(taskId: String): Option[PriorityQueueTaskState] = taskStates.get(taskId)

  /**
    * Not optimized version !
    * @param fact
    * @return
    */
  override def getTaskNamesForFactFromIndex(fact: Fact): Set[String] = taskStates.keys.toSet
}
