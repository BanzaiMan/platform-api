package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Identifier, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.idminter.utils.Identifiable
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class IdentifierGenerator @Inject()(dynamoDBClient: AmazonDynamoDB,
                                    dynamoConfig: DynamoConfig)
    extends Logging
    with TwitterModuleFlags {

  private val identifiersTableName = dynamoConfig.table

  def generateId(unifiedItem: UnifiedItem): Future[String] = Future {
    findMiroID(unifiedItem) match {
      case Some(identifier) => retrieveOrGenerateCanonicalId(identifier)
      case None =>
        logAndThrowError(s"Item $unifiedItem did not contain a MiroID")
    }
  }

  private def retrieveOrGenerateCanonicalId(identifier: SourceIdentifier) = {
    findMiroIdInDynamo(identifier.value) match {
      case Right(id) :: Nil => id.CanonicalID
      case Nil => generateAndSaveCanonicalId(identifier.value)
      case Right(_) :: tail =>
        logAndThrowError(
          s"Found more than one record with MiroID ${identifier.value}")
      case _ =>
        logAndThrowError(
          s"Error in parsing the object with MiroID ${identifier.value}")
    }
  }

  private def findMiroID(unifiedItem: UnifiedItem) =
    unifiedItem.identifiers.find(identifier => identifier.sourceId == "MiroID")

  private def findMiroIdInDynamo(miroId: String) = {
    Scanamo.queryIndex[Identifier](dynamoDBClient)(identifiersTableName,
                                                   "MiroID")('MiroID -> miroId)
  }

  private def generateAndSaveCanonicalId(miroId: String) = {
    val canonicalId = Identifiable.generate
    info(s"putting new canonicalId $canonicalId for MiroID $miroId")
    Scanamo.put(dynamoDBClient)(identifiersTableName)(
      Identifier(canonicalId, miroId))
    canonicalId
  }

  private def logAndThrowError(errorMessage: String) = {
    error(errorMessage)
    throw new Exception(errorMessage)
  }
}
