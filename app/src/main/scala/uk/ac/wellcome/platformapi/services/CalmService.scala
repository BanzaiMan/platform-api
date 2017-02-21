package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import uk.ac.wellcome.platform.api.models._


@Singleton
class CalmService @Inject()(
  elasticsearchService: ElasticsearchService
) {

  private def parentAltRefNo(altRefNo: String): String =
    altRefNo.take(altRefNo.lastIndexOf("/"))

  def findSMGRecordByAccessionId(accessionId: Int) = {
    elasticsearchService.client.execute {
      search("hackday2/records").query(
        boolQuery().must(
          matchQuery("identifier.type", "accession number"),
          matchQuery("identifier.value", accessionId.toString)
        )
      )
    }.map { _.hits.headOption.map { SMGRecord(_) }}
  }

  def findSMGRecordByAccessionRange(gte: Int, lte: Int): Future[List[SMGRecord]] = {
    elasticsearchService.client.execute {
      search("smg_wellcome/object").query(
        rangeQuery("identifier.value")
          .gte(gte)
          .lte(gte)
      )
    }.map { _.hits.map { SMGRecord(_) }.toList }
  }

//  def findRecordsByAccessionId(accessionId: Int): Future[List[SMGRecord]]

  def findParentCollectionByAltRefNo(altRefNo: String): Future[Option[Collection]] =
    findCollectionByAltRefNo(parentAltRefNo(altRefNo))

  def findRecordByAltRefNo(altRefNo: String): Future[Option[Record]] =
    elasticsearchService.client.execute {
      search("records/item").query(
        boolQuery().must(matchQuery("AltRefNo.keyword", altRefNo))
      )
    }.map { _.hits.headOption.map { Record(_) }}

  def findCollectionByAltRefNo(altRefNo: String): Future[Option[Collection]] = {
    elasticsearchService.client.execute {
      search("records/collection").query(
	boolQuery().must(matchQuery("AltRefNo.keyword", altRefNo))
      )
    }.map { _.hits.headOption.map { Collection(altRefNo, _) }}
  }
}
