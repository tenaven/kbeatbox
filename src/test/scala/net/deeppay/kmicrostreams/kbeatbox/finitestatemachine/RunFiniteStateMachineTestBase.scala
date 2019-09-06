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

import net.deeppay.kmicrostreams.kbeatbox.api.{InputEvent, Trace}
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
