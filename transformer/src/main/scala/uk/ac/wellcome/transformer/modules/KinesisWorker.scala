package uk.ac.wellcome.platform.transformer.modules

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ActorSystem, Props}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions._
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.twitter.inject.{Injector, Logging, TwitterModule}

import uk.ac.wellcome.platform.transformer.actors._
import uk.ac.wellcome.platform.transformer.modules._


object KinesisWorker extends TwitterModule {
  override val modules = Seq(
    StreamsRecordProcessorFactoryModule,
    KinesisClientLibConfigurationModule,
    DynamoConfigModule)

  val system = ActorSystem("KinesisWorker")

  val kinesisDynamoRecordExtractorActor =
    system.actorOf(Props[KinesisDynamoRecordExtractorActor], name="kdreactor")

  val dynamoCaseClassExtractorActor =
    system.actorOf(Props[DynamoCaseClassExtractorActor], name="dcceactor")

  override def singletonStartup(injector: Injector) {
    info("Starting Kinesis worker")

    val region = injector.instance[DynamoConfig].region

    val adapter = new AmazonDynamoDBStreamsAdapterClient(
      new DefaultAWSCredentialsProviderChain()
    )
    // TODO: weird stuff with Region[s]. Understand what's going on.
    // Should be able to do Regions.US_WEST_2
    adapter.setRegion(RegionUtils.getRegion(region))

    val kinesisConfig = injector.instance[KinesisClientLibConfiguration].withInitialPositionInStream(InitialPositionInStream.LATEST)

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS),
      new Worker(
        injector.instance[StreamsRecordProcessorFactory],
        kinesisConfig,
        adapter,
        AmazonDynamoDBClientBuilder
          .standard()
          .withRegion(region)
          .build(),
        AmazonCloudWatchClientBuilder
          .standard()
          .withRegion(region)
          .build()
      )
    )

  }

  override def singletonShutdown(injector: Injector) {
    info("Shutting down Kinesis worker")
    system.terminate()
  }
}
