package uk.ac.wellcome.platform.reindexer

import com.gu.scanamo.Scanamo
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{CalmTransformable, Reindex}
import uk.ac.wellcome.test.utils.{ExtendedPatience, TestFlags}

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience with TestFlags {

  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.reindexTracker.tableName" -> "ReindexTracker",
        "aws.dynamo.miroData.tableName" -> "CalmData",
        "reindex.target.tableName" -> "CalmData"
      ) ++ testFlags
    )

  it(
    "should increment the reindexVersion to the value requested on all items of a table in need of reindex"
  ) {

    val currentVersion = 1
    val requestedVersion = 2

    val calmTransformableList = List(
      CalmTransformable(
        RecordID = "RecordID1",
        RecordType = "Collection",
        AltRefNo = "AltRefNo1",
        RefNo = "RefNo1",
        data = """{"AccessStatus": ["public"]}""",
        ReindexVersion = currentVersion
      ))

    val expectedCalmTransformableList = calmTransformableList
      .map(_.copy(ReindexVersion = requestedVersion))
      .map(Right(_))

    val reindexList = List(
      Reindex(calmDataTableName, requestedVersion, currentVersion)
    )

    calmTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(calmDataTableName))

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    server.start()

    eventually {
      Scanamo.scan[CalmTransformable](dynamoDbClient)(calmDataTableName) shouldBe expectedCalmTransformableList

      server.httpGet(path = "/management/healthcheck",
                     andExpect = Created,
                     withJsonBody = """{"message": "success"}""")
    }
  }
}
