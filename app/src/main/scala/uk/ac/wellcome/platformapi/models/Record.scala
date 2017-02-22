package uk.ac.wellcome.platform.api.models

import com.sksamuel.elastic4s.searches.RichSearchHit
import scala.util.Try

case class SMGRecord(
  title: String,
  uri: String,
  imageUri: Option[String]
)
case object SMGRecord {
  def apply(hit: RichSearchHit): SMGRecord = {
    val data: Map[String, AnyRef] =
      hit.sourceAsMap.filter(o => o._2 != null)

    val admin = data("admin").asInstanceOf[java.util.HashMap[String, AnyRef]]
    val uid = admin.get("uid").toString

    SMGRecord(
      title=data("summary_title").toString,
      uri=s"http://collection.sciencemuseum.org.uk/objects/${uid}",
      imageUri=None
    )
  }
}

case class AccessionRange(from: Int, to: Int)

case class Record(
  altRefNo: String,
  title: String,
  materialType: String,
  date: Option[String],
  acquisition: Option[String],
  accessionRange: Option[AccessionRange]
)
case object Record {
  def apply(hit: RichSearchHit): Record = {
    val data: Map[String, AnyRef] =
      hit.sourceAsMap.filter(o => o._2 != null)


    val accRange = Try {
      data("accrange").asInstanceOf[java.util.HashMap[String, Int]]
    }.map(range =>
      AccessionRange(range.get("gte"), range.get("lte"))
    ).toOption

    Record(
      altRefNo=data("AltRefNo").toString,
      title=data("Title").toString,
      materialType=data("Material").toString,
      date=data.get("Date").map(_.toString),
      acquisition=data.get("Acquisition").map(_.toString),
      accessionRange=accRange
    )
  }
}
