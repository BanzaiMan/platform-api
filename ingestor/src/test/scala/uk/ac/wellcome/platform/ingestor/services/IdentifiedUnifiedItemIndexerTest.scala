package uk.ac.wellcome.platform.ingestor.services

import com.fasterxml.jackson.core.JsonParseException
import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class IdentifiedUnifiedItemIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticSearchLocal {

  it("should insert an identified unified item into Elasticsearch") {
    val index = "records"
    val itemType = "item"
    val identifiedUnifiedItemIndexer =
      new IdentifiedUnifiedItemIndexer(index, itemType, elasticClient)
    val identifiedUnifiedItemString = JsonUtil
      .toJson(
        IdentifiedUnifiedItem(
          canonicalId = "5678",
          unifiedItem = UnifiedItem(
            identifiers = List(SourceIdentifier("Miro", "MiroID", "1234")), label = "some label")))
      .get

    val future = identifiedUnifiedItemIndexer.indexIdentifiedUnifiedItem(
      identifiedUnifiedItemString)

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$index/$itemType").matchAll().limit(100))
          .map { _.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedUnifiedItemString
      }
    }

  }

  it("should return a failed future if the input string is not an identified unified item") {
    val identifiedUnifiedItemIndexer =
      new IdentifiedUnifiedItemIndexer("records", "item", elasticClient)
    val future = identifiedUnifiedItemIndexer.indexIdentifiedUnifiedItem("a document")

    whenReady(future.failed) { exception =>
      exception shouldBe a[JsonParseException]
    }
  }
}