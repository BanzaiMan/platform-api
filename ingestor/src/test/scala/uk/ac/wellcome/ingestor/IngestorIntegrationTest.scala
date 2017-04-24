package uk.ac.wellcome.ingestor

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.testkit.ElasticSugar
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.ingestor.modules.SQSWorker
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil
import com.sksamuel.elastic4s.ElasticDsl._

class IngestorIntegrationTest
    extends IntegrationTest
    with SQSLocal
    with Eventually
    with IntegrationPatience {

  override def queueName: String = "es_ingestor_queue"

  override def injector: Injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.sqs.queue.url" -> queueUrl,
        "aws.sqs.waitTime" -> "1",
        "es.host" -> "localhost",
        "es.port" -> 9300.toString,
        "es.name" -> "wellcome",
        "es.xpack.enabled" -> "true",
        "es.xpack.user" -> "elastic:changeme",
        "es.xpack.sslEnabled" -> "false",
        "es.sniff" -> "false",
        "es.index" -> "records",
        "es.type" -> "item"
      ),
      modules = Seq(SQSConfigModule,
        AkkaModule,
        SQSReaderModule,
        SQSWorker,
        SQSLocalClientModule,
        ElasticClientModule)
    )

  val elasticClient = injector.instance[ElasticClient]
  elasticClient.execute(createIndex("records")).await
  test("it should read an identified unified item from the SQS queue and ingest it into elastic search") {
    val identifiedUnifiedItem = JsonUtil
      .toJson(
        IdentifiedUnifiedItem(
          canonicalId = "1234",
          unifiedItem = UnifiedItem(
            identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")))))
      .get

    sqsClient.sendMessage(
      queueUrl,
      JsonUtil
        .toJson(
          SQSMessage(Some("identified-item"),
                     identifiedUnifiedItem,
                     "ingester",
                     "messageType",
                     "timestamp"))
        .get
    )

    SQSWorker.singletonStartup(injector)

    eventually {
      val hits =
        elasticClient.execute(search("records/item").matchAll()).map(_.hits).await
      hits should have size 1
      hits.head.sourceAsString shouldBe identifiedUnifiedItem
    }
  }
}
