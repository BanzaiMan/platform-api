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

  def findSMGRecordsByAccessionId(accessionId: Int): Future[List[SMGRecord]] = for {
    recordOption <- findRecordByAccessionId(accessionId)
    if !recordOption.isEmpty
    record = recordOption.get
    if !record.accessionRange.isEmpty
    range = record.accessionRange.get
    smgRecords <- findSMGRecordByAccessionRange(range.from, range.to)
  } yield smgRecords

  def findCollectionsFreeText(q: String) =
    elasticsearchService.client.execute {
      search("hackday3/records").query(
        simpleStringQuery(q)
      )
    }.map { _.hits.map { Collection("foo", _) }.toList }

  def findRecordByAccessionId(accessionId: Int) = {
    elasticsearchService.client.execute {

     val rangeQ = s"""
       {
         "range": {
           "accrange": {
             "gte": "${accessionId}",
             "lte": "${accessionId}",
             "relation": "within"
           }
         }
       }"""

      search("hackday2/records").rawQuery(rangeQ)
    }.map { _.hits.headOption.map { Record(_) }}
  }

  def findSMGRecordByAccessionRange(from: Int, to: Int): Future[List[SMGRecord]] = {
    elasticsearchService.client.execute {

      search("smg_wellcome/object").query(
        rangeQuery("identifier.value")
          .gte(from)
          .lte(to)
      )
    }.map { _.hits.map { SMGRecord(_) }.toList }
  }

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
