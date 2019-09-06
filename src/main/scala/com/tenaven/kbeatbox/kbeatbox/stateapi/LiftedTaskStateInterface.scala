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
package com.tenaven.kbeatbox.kbeatbox.stateapi

import com.tenaven.kbeatbox.kbeatbox.api.Fact

/**
  * Minimal state API for use in the LiftTaskStateFiniteStateMachine
  */
trait LiftedTaskStateInterface {
  def getTaskForId(taskId: String) : Option[PriorityQueueTaskState]

  /**
    * Takes a list of tuples of (name, options of task) to update. When the option is None,
    * then the task is to be removed from the State.
    */
  def updateAListOfTasks(updatedTasks: List[(String, Option[PriorityQueueTaskState])]): this.type
  def getTaskNamesForFactFromIndex(fact : Fact): Set[String]
}
