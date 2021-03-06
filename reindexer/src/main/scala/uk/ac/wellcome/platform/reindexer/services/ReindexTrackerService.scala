package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class ReindexTrackerService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig],
  @Flag("reindex.target.tableName") reindexTargetTableName: String)
    extends Logging {

  private val reindexTrackerTableConfigId = "reindex"

  private val reindexTrackerConfig = dynamoConfigs.getOrElse(
    reindexTrackerTableConfigId,
    throw new RuntimeException(
      s"ReindexTracker ($reindexTrackerTableConfigId) dynamo config not available!"))

  private val reindexTrackerTableName = reindexTrackerConfig.table
  private val reindexTable = Table[Reindex](reindexTrackerTableName)

  def updateReindex(reindexAttempt: ReindexAttempt) = Future {
    val updatedReindex = reindexAttempt.reindex.copy(
      CurrentVersion = reindexAttempt.reindex.RequestedVersion)

    info(
      s"Attempting to update ReindexTracker record: $reindexAttempt -> $updatedReindex")

    Scanamo.put[Reindex](dynamoDBClient)(reindexTrackerTableName)(
      updatedReindex)
  }

  def getIndexForReindex: Future[Option[Reindex]] =
    getIndices.map {
      case Reindex(tableName, requested, current) if requested > current => {
        info(
          s"ReindexTracker found out of sync table: $tableName, ($requested > $current)")
        Some(Reindex(tableName, requested, current))
      }
      case _ => {
        info(s"ReindexTracker found no out of sync tables.")
        None
      }
    }

  private def getIndices: Future[Reindex] = Future {
    Scanamo.exec(dynamoDBClient)(
      reindexTable.get('TableName -> reindexTargetTableName)) match {
      case Some(Right(reindex)) => {
        info(s"ReindexTracker found $reindex matching $reindexTargetTableName")
        reindex
      }
      case Some(Left(dynamoReadError)) =>
        throw new RuntimeException(
          s"Unable to read from $reindexTrackerTableName: $dynamoReadError")
      case None =>
        throw new RuntimeException(
          s"No table matching $reindexTargetTableName found")
    }
  }
}
