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
package net.deeppay.kmicrostreams.kbeatbox.stateapi

import net.deeppay.kmicrostreams.kbeatbox.api.Fact

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
