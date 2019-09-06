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
