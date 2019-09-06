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

import java.io.FileWriter

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, Condition, HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask}
import com.tenaven.kbeatbox.kbeatbox.mock.{LiftedTaskMockState, MockConcreteLiftedTaskStateFiniteStateMachine}
import com.tenaven.kbeatbox.kbeatbox.stateapi.PriorityQueueTaskState
import com.typesafe.scalalogging.Logger
import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable

/**
  * TODO Add some advanced serialization to the State so it is beautiful.
  */

/**
  * This class tests for all possible scenarios that would affect code
  * expected performance
  */
class KBeatboxOutputPumlTest extends WordSpec with Matchers {
  val logger = Logger(classOf[KBeatboxOutputPumlTest])
  def runFiniteStateMachine[State, Event, T](finiteStateMachine: MealyFiniteStateMachine[State, Event, T], s: Stream[Event]): Stream[(State, List[T])] = {
    s.scanLeft((finiteStateMachine.zero(), List[T]()))(
      (st, evt) => finiteStateMachine.transition(st._1, evt))
  }

  def printColumns(columns: List[String], backgroundColor: String, textColor: String): String = {
    s"<$backgroundColor>| " + columns.map(s"<color:$textColor>" + _ + "</color>" ).iterator.mkString(" | ") + " |"
  }

  " be able to receive Ack events, and chain multiple AckEvents with another event" in {
    val eventStream: Stream[InputEvent] = Stream(ScheduleTask("A", Set(HeartBeatCondition(1L))),
      ScheduleTask("B", Set(HeartBeatCondition(2L))),
      ScheduleTask("C", Set(AckEventCondition("A"), AckEventCondition("B"))),
      HeartBeat(1L),
      HeartBeat(2L),
      AckEvent("A"),
      AckEvent("B"),
      AckEvent("C"),
      ScheduleTask("A", Set(HeartBeatCondition(3L))),
      ScheduleTask("A", Set(HeartBeatCondition(4L))),
      ScheduleTask("A", Set())
    )


    val finiteStateMachine = new MockConcreteLiftedTaskStateFiniteStateMachine()

    val stateStream: Stream[LiftedTaskMockState] = runFiniteStateMachine(finiteStateMachine, eventStream) map(_._1.asInstanceOf[LiftedTaskMockState])

    hash(stateStream(1))

    val transitionStream: immutable.Seq[(LiftedTaskMockState, InputEvent, LiftedTaskMockState)] = (stateStream zip(eventStream zip( stateStream.tail ))).map(t => (t._1, t._2._1, t._2._2))
    val sb = new StringBuffer()
    sb.append("@startuml\n")
    sb.append("!include ../style.puml\n")
    val allStates = stateStream.map( state => (hash(state) -> state)).toMap
    allStates.foreach(stateTuple => {
      val (key :String, state) = stateTuple
      sb.append(s"state state_${key}\n")
      //print(s"state state_${key}\n")
      val allTasks = state.taskStates.map(kvPair => {
        val (taskId, singleTaskState) = kvPair
        (taskId, (singleTaskState, if (singleTaskState.isTriggered) "running" else "scheduled"))
      })

      if (!allTasks.isEmpty) {
        printState(sb, key, allTasks)
      }


    })

    printTransitions(sb, transitionStream)

    sb.append(s"note left of state_${hash(stateStream(10))}\n")
    sb.append("Usecase : update a scheduled task.\nendnote\n")
    sb.append(s"\nnote top of state_${hash(stateStream(1))}\n")
    sb.append("Usecase : create three tasks A, B, C. C is dependent on A **and** B.\n")
    sb.append("endnote\n")
    sb.append(s"center footer `lastHeartBeatReceived` in the state was voluntarily omitted to have cycles\n")
    sb.append("@enduml\n")

    var fileWriter : FileWriter = null
    try {
      fileWriter = new FileWriter("target/kbeatbox_state_diagram.puml")
      fileWriter.append(sb.toString)
    } finally {
      if (fileWriter != null) {
        fileWriter.close()
      }
    }

  }


  private def printTransitions(sb : StringBuffer, transitionStream: immutable.Seq[(LiftedTaskMockState, InputEvent, LiftedTaskMockState)]) = {
    transitionStream.foreach(tuple => {
      val (from, label, to) = tuple
      sb.append(s"state_${hash(from)} --> state_${hash(to)} : ${label}\n")
    })
  }

  private def printState(sb: StringBuffer, key: String, allTasks: Map[String, (PriorityQueueTaskState, String)]): Unit = {
    sb.append(s"state_${key} : ")
    sb.append(printColumns(List("name", "top of queue conditions", "status"), "#033A5E", "white"))
    sb.append("\n")
    allTasks.zipWithIndex.foreach(tuple => {
      val ((_, (task, status)), index) = tuple
      if (!task.taskName.equals("")) {

        val (backgroundColor, textColor) = if (index % 2 == 0) ("#BFDDF0", "black") else ("#60AAD9", "black")
        val conditions: Option[Set[Condition]] = task.priorityQueue.lastOption.map(_._2)
        if (conditions.isEmpty) {
          sb.append(s"state_${key} : ")
          sb.append(printColumns(List(task.taskName, "-", status), backgroundColor, textColor))
          sb.append("\n")
        } else {
          val conditionsWithIndex: Set[(Condition, Int)] = conditions.get.zipWithIndex
          conditionsWithIndex.foreach(conditionWithIndex => {
            val (condition, index) = conditionWithIndex
            sb.append(s"state_${key} : ")
            if (index == 0) {
              sb.append(printColumns(List(task.taskName, condition.toString, status), backgroundColor, textColor))
            } else {
              sb.append(printColumns(List(" ", condition.toString, " "), backgroundColor, textColor))
            }
            sb.append("\n")
          })
        }
      }
    })
  }

  private def hash(state: LiftedTaskMockState) = {
    val hash = state.taskStates.values.filter(p => !p.taskName.equals("") && !p.priorityQueue.isEmpty).foldLeft(0L)((h : Long, state: PriorityQueueTaskState) => h ^ state.hashCode())
    val padded: String = hash.toHexString.toUpperCase.reverse.padTo[Char, String](16, '0').reverse
    padded
  }
}