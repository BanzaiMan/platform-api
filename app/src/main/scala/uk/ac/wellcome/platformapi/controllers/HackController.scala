package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.platform.api.services._

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.Future
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.NotEmpty

import uk.ac.wellcome.platform.api.models._

case class SearchRequest(
  @NotEmpty @QueryParam q: String
)

case class AccessionRequest(
  @NotEmpty @QueryParam accessionId: String
)

case class AccessionRangeRequest(
  @NotEmpty @QueryParam from: String,
  @NotEmpty @QueryParam to: String
)


case class AccessionResponse(
  current: SMGRecord,
  adjacentAccessions: List[SMGRecord]
)

case class Subject(
  name: String,
  uri: String
)

case class CollectionResponse(
  collections: List[Collection],
  subjects: List[Subject]
)

@Singleton
class HackController @Inject()(
  calmService: CalmService
) extends Controller {

  val apiBaseUrl = "/api/hack"

  get(s"${apiBaseUrl}/collection") { request: SearchRequest =>
    calmService.findCollectionsFreeText(request.q).map { collectionList =>
      if(collectionList.isEmpty) {
        response.ok.json(Nil)
      } else {
        val dummySubjects = List(Subject("Cheese", "http://www.example.com"))
        response.ok.json(CollectionResponse(collectionList,dummySubjects))
      }
    }
  }


  get(s"${apiBaseUrl}/smg_record/accession") { request: AccessionRequest =>
    calmService.findSMGRecordsByAccessionId(request.accessionId.toInt).map { recordList =>
      if(recordList.isEmpty) {
        response.notFound
      } else {
        response.ok.json(recordList)
      }
    }
  }

  get(s"${apiBaseUrl}/smg_record") { request: AccessionRangeRequest =>
    calmService.findSMGRecordByAccessionRange(
      request.from.toInt,
      request.to.toInt
    ).map { recordList =>
      if(recordList.isEmpty) {
        response.notFound
      } else {
        response.ok.json(recordList)
      }
    }
  }

  get(s"${apiBaseUrl}/record") { request: AccessionRequest =>
    calmService.findRecordByAccessionId(request.accessionId.toInt).map { recordOption =>
     recordOption
        .map(response.ok.json)
	.getOrElse(response.notFound)
    }
  }
}
