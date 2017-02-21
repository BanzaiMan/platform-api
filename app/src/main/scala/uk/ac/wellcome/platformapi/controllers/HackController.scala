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

case class SMGRecord(
  title: String,
  uri: String,
  imageUri: Option[String]
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
    val current = SMGRecord(
      "Launcher, Black Arrow (incomplete)",
      "http://collection.sciencemuseum.org.uk/objects/co40246/black-arrow-r4-launch-vehicle-1971-rockets-satellite-launchers-launch-vehicles",
      Some("http://smgco-images.s3.amazonaws.com/media/W/P/A/medium_1972_0325__0002_.jpg"))

    val record = AccessionResponse(current, List(current,current,current))

    response.ok.json(record)
  }
}
