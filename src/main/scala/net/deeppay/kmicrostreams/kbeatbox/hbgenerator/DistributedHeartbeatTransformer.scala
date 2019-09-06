package net.deeppay.kmicrostreams.kbeatbox.hbgenerator


import java.util.UUID

import com.typesafe.scalalogging.Logger
import net.deeppay.kmicrostreams.kbeatbox.api.{HeartBeat, InputEvent}
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.{Cancellable, ProcessorContext, PunctuationType, Punctuator}

case class DBGeneratorConfiguration(restingPeriod: Long = 1000,
                                    interval: Long = 1000,
                                   topic: String = "heartBeatEvent",
                                   brokers:String = "localhost:9092",
                                   numOfPartition: Int = 1,
                                   nodes: Map[String, String] = Map.empty
                                  )
class DistributedHeartbeatPunctuator(context: ProcessorContext) extends Punctuator
{
  val logger = Logger(classOf[DistributedHeartbeatPunctuator])

  def punctuate (timestamp: Long): Unit = {
    val currentTime = System.currentTimeMillis()

    logger.info(s"Heartbeat currentTime: [$currentTime] taskId : [${context.taskId()}] applicationId : [${context.applicationId()}]")
    val heartbeat = HeartBeat(currentTime)
    val key = UUID.randomUUID().toString
    context.forward(key, heartbeat)
    context.commit()
  }
}

class DistributedHeartbeatTransformer(puntuatorConfig: DBGeneratorConfiguration) extends Transformer[String, String, (String, InputEvent)] {
  var runningPunctuator: Cancellable = _
  val logger = Logger(classOf[DistributedHeartbeatTransformer])

  var context: ProcessorContext = _

  override def init(context: ProcessorContext): Unit = {
     this.context = context
     this.runningPunctuator = this.context.schedule(puntuatorConfig.interval, PunctuationType.WALL_CLOCK_TIME, (new DistributedHeartbeatPunctuator(context)))
   }
  override def transform(key: String, value: String): (String, InputEvent) = ???

  override def close(): Unit = {
     logger.info("Distributed heartbeat closed. Stopping punctuator")
     this.runningPunctuator.cancel()
   }
  override def punctuate(timestamp: Long): (String, InputEvent) = ???
}
