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

import com.tenaven.kbeatbox.kbeatbox.api.{InputEvent, Trace}
import org.scalatest.junit.AssertionsForJUnit

trait RunFiniteStateMachineTestBase extends AssertionsForJUnit {
  val finiteStateMachines : List[MealyFiniteStateMachine[_, InputEvent, Trace]] = List()

  def runFiniteStateMachine[State, Event, T](finiteStateMachine: MealyFiniteStateMachine[State, Event, T],
                                             inputStream: Stream[Event]): Stream[(State, List[T])] = {
    inputStream.scanLeft((finiteStateMachine.zero(), List[T]()))(
      (st, evt) => finiteStateMachine.transition(st._1, evt))
  }
  def runTestAndAssertTraces(s: Stream[InputEvent],
                             expectTraces: Stream[List[Trace]],
                             currentTimeRemoved : Boolean = true,
                             specificFiniteStateMachines : List[MealyFiniteStateMachine[_, InputEvent, Trace]] = finiteStateMachines,
                             specificAssertOnFinalState : (Any, MealyFiniteStateMachine[_, InputEvent, Trace]) => Unit = ( (x, y) => {})
                            ): Unit = {
    specificFiniteStateMachines.foreach(
      finiteStateMachine => {
        val resultingStream: Stream[(Any, List[Trace])] = runFiniteStateMachine(finiteStateMachine, s)
        resultingStream.lastOption.foreach( tuple => specificAssertOnFinalState(tuple._1, finiteStateMachine) )
        assert( resultingStream.map(_._2.map((trace :Trace) => if (currentTimeRemoved) {
          trace.update(None)
        } else {
          trace
        })) === expectTraces, s"FSM ${finiteStateMachine.getClass}")
      }
    )
  }
}
