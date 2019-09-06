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

import com.tenaven.kbeatbox.kbeatbox.api.{Fact, InputEvent, ScheduleTask, Trace}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{LiftedTaskStateInterface, PriorityQueueTaskState}


/**
  * This is a lift (in that sense : http://learnyouahaskell.com/functors-applicative-functors-and-monoids)
  * of the SingleTaskStateFiniteStateMachine to accept multiple
  * named Tasked. This is not an efficient version and only exists for clarity & testing purposes.
  * Use ArmoringHeartBeatFiniteStateMachine instead for real purposes.
  */
abstract class LiftedTaskStateFiniteStateMachine[T <: LiftedTaskStateInterface] extends MealyFiniteStateMachine[T, InputEvent, Trace] {

  val underlyingFiniteStateMachine : MealyFiniteStateMachine[PriorityQueueTaskState, InputEvent, Trace] = new PriorityQueueTaskStateFiniteStateMachine()




  override def updateTask(sourceState: T, task: ScheduleTask) : (T, List[Trace]) = {
    val maybeInitialPriorityQueueState: Option[PriorityQueueTaskState] = sourceState.getTaskForId(task.taskId)

    val initialPriorityQueueState = maybeInitialPriorityQueueState.getOrElse(underlyingFiniteStateMachine.zero())

    val (updatedSingleTaskState, traces) = underlyingFiniteStateMachine.updateTask(initialPriorityQueueState, task)

    val stateWithIndexUpdated: T = updateIndexOnTaskIfNecessary(sourceState, task, maybeInitialPriorityQueueState)

    (
    stateWithIndexUpdated.updateAListOfTasks(List(task.taskId -> clearTaskIfPriorityQueueIsEmpty(updatedSingleTaskState))),
    traces
    )
  }

  private def clearTaskIfPriorityQueueIsEmpty(updatedSingleTaskState: PriorityQueueTaskState): Option[PriorityQueueTaskState] = {
    if (updatedSingleTaskState.priorityQueue.isEmpty) None else Some(updatedSingleTaskState)
  }

  /**
    * this is a stub for IndexOptimizedLiftedTaskStateFiniteStateMachine to implement
    */
  def updateIndexOnTaskIfNecessary(sourceState: T, task: ScheduleTask, maybeInitialPriorityQueueState: Option[PriorityQueueTaskState]): T = sourceState



  /**
    * This is really slow because we do a whole pass across all tasks in the map.
    */
  override def acceptFact(sourceState: T, fact: Fact) : (T, List[Trace]) = {

    val taskNamesToUpdate = sourceState.getTaskNamesForFactFromIndex(fact)

    val resultingTasksAndTracesFromAcceptingFact = acceptFactOnTasksToUpdate(sourceState, fact, taskNamesToUpdate.toSeq)

    val newIndexState = updateIndexForResultingTasks(sourceState, resultingTasksAndTracesFromAcceptingFact)

    val resultingState = newIndexState.updateAListOfTasks(resultingTasksAndTracesFromAcceptingFact.map {
      case (taskName, resultingPriorityQueueTaskState, _, isFinished) => (taskName, if (isFinished) None else Some(resultingPriorityQueueTaskState))
    })

    (
      resultingState,
      resultingTasksAndTracesFromAcceptingFact.flatMap(_._3)
    )
  }
  
  /**
    * this is a stub for the IndexOptimizedLiftedTaskStateFiniteStateMachine to actually implement.
    */
  def updateIndexForResultingTasks(sourceState: T, resultingTasksAndTracesFromAcceptingFact: List[(String, PriorityQueueTaskState, List[Trace], Boolean)]): T =  sourceState


  private def acceptFactOnTasksToUpdate(sourceState :T, fact: Fact, taskNamesToUpdate: Seq[String]): List[(String, PriorityQueueTaskState, List[Trace], Boolean)] = {
    taskNamesToUpdate.flatMap(
      taskName => {
        sourceState.getTaskForId(taskName) map { transitionTaskEntryBasedOnFact(fact, taskName) }
      }
    ).toList
  }

  private def transitionTaskEntryBasedOnFact(fact: Fact, taskName: String)(previousTaskState : PriorityQueueTaskState): (String, PriorityQueueTaskState, List[Trace], Boolean) = {

    val (nextTaskState, traces) = underlyingFiniteStateMachine.acceptFact(previousTaskState, fact)

    val isFinished = isFinishedTaskTransition(previousTaskState, nextTaskState)
    (taskName, nextTaskState, traces, isFinished)
  }

  private def isFinishedTaskTransition(previousTaskState: PriorityQueueTaskState, nextTaskState: PriorityQueueTaskState): Boolean = {
    previousTaskState.isTriggered && !nextTaskState.isTriggered
  }

}
