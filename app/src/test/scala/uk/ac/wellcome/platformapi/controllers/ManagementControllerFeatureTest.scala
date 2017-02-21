package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.platform.api.Server

class ManagementControllerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new Server)

  "Server" should {
    "respond" in {
      server.httpGet(
        path = "/management/healthcheck",
        andExpect = Ok,
        withBody = "{\"message\":\"ok\"}")
    }
  }
}
