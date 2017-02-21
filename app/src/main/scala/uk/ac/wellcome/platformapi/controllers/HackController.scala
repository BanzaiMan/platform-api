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


case class AccessionRequest(
  @NotEmpty @QueryParam accessionId: String
)

case class AccessionResponse(
  current: SMGRecord,
  adjacentAccessions: List[SMGRecord]
)

@Singleton
class HackController @Inject()(
  calmService: CalmService
) extends Controller {

  val apiBaseUrl = "/api/hack"

  get(s"${apiBaseUrl}/record") { request: AccessionRequest =>
    calmService.findSMGRecordByAccessionId(request.accessionId.toInt).map { recordOption =>
     recordOption
        .map(response.ok.json)
	.getOrElse(response.notFound)
    }


  }
}
