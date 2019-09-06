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
package com.tenaven.kbeatbox.kbeatbox.kafkaintegration

import com.tenaven.kbeatbox.kbeatbox.api.{InputEvent, Trace}
import com.tenaven.kbeatbox.kbeatbox.finitestatemachine.{ArmoringHeartBeatFiniteStateMachine, HeartBeatKeepingFiniteStateMachine}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{HeartBeatKeepingStateInterface, PriorityQueueTaskState}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore




class KafkaArmoringHeartBeatFiniteStateMachine(val context : ProcessorContext) extends ArmoringHeartBeatFiniteStateMachine {

  override def zero(): HeartBeatKeepingStateInterface = {
    val kafkaState = new HeartBeatKeepingKafkaState()
    kafkaState.heartBeatStateStore = context.getStateStore(KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore).asInstanceOf[KeyValueStore[String, Long]]
    kafkaState.ackIndex = context.getStateStore(KBeatboxKafkaStateStoreConstants.KBeatboxAckEventIndex).asInstanceOf[KeyValueStore[String, Set[String]]]
    kafkaState.hbIndex  = context.getStateStore(KBeatboxKafkaStateStoreConstants.KBeatboxhbIndex).asInstanceOf[KeyValueStore[Long, Set[String]]]
    kafkaState.taskStates = context.getStateStore(KBeatboxKafkaStateStoreConstants.KBeatboxTaskStore).asInstanceOf[KeyValueStore[String, PriorityQueueTaskState]]
    kafkaState
  }
}
/**
  * Integrate the State.transition function into a Kafka Transformer.
  */
class KbeatboxKafkaTransformer extends Transformer[String, InputEvent, (String, Trace)] {

  val logger = Logger(classOf[KbeatboxKafkaTransformer])

  var finiteStateMachine : HeartBeatKeepingFiniteStateMachine = _
  var context: ProcessorContext = _
  var state : HeartBeatKeepingStateInterface = _

  /**
    * initialise the local context variable and
    * initialise the stateStore.
    * @param context
    */
  override def init(context: ProcessorContext): Unit = {
    this.context = context
    this.finiteStateMachine = new KafkaArmoringHeartBeatFiniteStateMachine(context)
    this.state = this.finiteStateMachine.zero()
  }

  override def close(): Unit = {}

  /**
    * Take the inputEvents from the input topic and transform them into traces by transitioning the current state
    * stored in the stateStore to a new State.
    *
    * @param key
    * @param inputEvent
    * @return
    */

  override def transform(key: String, inputEvent: InputEvent): (String, Trace) = {


    // get the current state from the stateStore or if empty initialize the State to zero.
    val beforeState: HeartBeatKeepingStateInterface = this.state

    logger.info(s"Starting transition inputEvent [${inputEvent}]")
    val (_, traces) = finiteStateMachine.transition(beforeState, inputEvent) // transition to afterState

    traces.foreach(trace => {
      logger.info(s"Trace : [${trace}]")
    })

    traces.foreach(trace => {
      forwardTrace(key, trace)
    })
    logger.info(s"Finished transition inputEvent [${inputEvent}]")
    context.commit()
    null // this is necessary as the Transformer normally returns a message for the out topic. The `null` forwards nothing.
  }


  def forwardTrace(key: String, trace: Trace): Unit = {
    context.forward(key, trace)
  }

  override def punctuate(timestamp: Long): (String, Trace) = ???
}
