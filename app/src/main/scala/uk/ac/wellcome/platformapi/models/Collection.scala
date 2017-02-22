package uk.ac.wellcome.platform.api.models

import com.sksamuel.elastic4s.searches.RichSearchHit


case class Collection(
  refno: String,
  title: String,
  calmUri: String
)
case object Collection {
  def apply(altRefNo: String, esResult: RichSearchHit): Collection = {
    val data = esResult.sourceAsMap
    val altRefNoFromData = data("AltRefNo").toString

    val calmBaseUrl = "http://archives.wellcomelibrary.org"
    val pathPattern = "/DServe/dserve.exe?dsqIni=Dserve.ini&dsqApp=Archive&dsqCmd=Show.tcl&dsqDb=Catalog&dsqPos=0&dsqSearch=(AltRefNo=%1$s)"
    val uriPattern = calmBaseUrl + pathPattern

    val calmUri = uriPattern.format(altRefNoFromData)

    Collection(altRefNoFromData, data("Title").toString, calmUri)
  }
}
