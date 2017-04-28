package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.ingestor.modules.SQSWorker
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class IngestorFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with SQSLocal
    with Matchers
    with ElasticSearchLocal with ScalaFutures{

  override def queueName: String = "test_es_ingestor_queue"
  val indexName = "records"
  val itemType = "item"

  override val server = new EmbeddedHttpServer(
    new Server() {
      override val modules = Seq(SQSConfigModule,
                                 AkkaModule,
                                 SQSReaderModule,
                                 SQSWorker,
                                 SQSLocalClientModule,
                                 ElasticClientModule)
    },
    flags = Map(
      "aws.region" -> "eu-west-1",
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "es.host" -> "localhost",
      "es.port" -> "9300",
      "es.name" -> "wellcome",
      "es.xpack.enabled" -> "true",
      "es.xpack.user" -> "elastic:changeme",
      "es.xpack.sslEnabled" -> "false",
      "es.sniff" -> "false",
      "es.index" -> indexName,
      "es.type" -> itemType
    )
  )

  it("should read an identified unified item from the SQS queue and ingest it into Elasticsearch") {
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

    eventually {
      val hitsFuture = elasticClient.execute(search(s"$indexName/$itemType").matchAll()).map(_.hits)
      whenReady(hitsFuture) { hits =>
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedUnifiedItem
      }
    }
  }
}
