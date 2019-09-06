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
package net.deeppay.kmicrostreams.kbeatbox.finitestatemachine

import net.deeppay.kmicrostreams.kbeatbox.api.{Fact, InputEvent, ScheduleTask, Trace}
import net.deeppay.kmicrostreams.kbeatbox.stateapi._


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
