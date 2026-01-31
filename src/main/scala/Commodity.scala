import upickle.{ReadWriter, macroRW}
import ujson.Obj
import java.math.MathContext
import java.math.RoundingMode

case class AvocadoPrice(
  region: String,
  variety: String,
  organic: String,
  averagePrice: Double,
  previousPrice: Option[Double]
) {

  def priceChange = previousPrice.filter(_ != 0d).map: price =>
    val c = BigDecimal(averagePrice)
    val p = BigDecimal(price)
    val percentChange = ((c - p) / p) * 100
    percentChange.round(new MathContext(2, RoundingMode.HALF_UP))

  def asSeq: Seq[String] = Seq(
    region,
    variety,
    organic,
    f"${averagePrice}%.2f",
    previousPrice.map(p => f"${p}%.2f").getOrElse("-"),
    priceChange match {
      case Some(p) => (if (p >= 0) "+" else "") + p.toString + "%"
      case None => "-"
    }
  )
}

case class ReportHeader(
  results: List[Header]
)

object ReportHeader:
  implicit val rw: ReadWriter[ReportHeader] = macroRW

case class Header(
  report_end_date: String
)

object Header:
  implicit val rw: ReadWriter[Header] = macroRW

case class ReportDetails(
  reportSection: String,
  reportSections: List[String],
  results: List[Commodity]
)

object ReportDetails:
  implicit val rw: ReadWriter[ReportDetails] = macroRW


case class Commodity(
  commodity: String,
  report_begin_date: String,
  report_end_date: String,
  region: String,
  size: String,
  variety: String,
  organic: String,
  prior_WK_wtd_avg_price: Option[Double],
  wtd_avg_price: Double
) {
  def asSeq: Seq[String] = Seq(region, variety, organic, wtd_avg_price.toString)
}

object Commodity:
  implicit val rw: ReadWriter[Commodity] = macroRW

case class WebhookTable(
  text: String,
  blocks: List[WebhookBlock]
)

object WebhookTable{
  implicit val rw: ReadWriter[WebhookTable] = macroRW
}

case class WebhookBlock(
  `type`: String,
  columns: Option[List[WebhookRow]],
  rows: Option[List[List[WebhookRow]]],
  text: Option[WebhookText]
)

object WebhookBlock {
  implicit val rw: ReadWriter[WebhookBlock] = macroRW
}

case class WebhookText(
  `type`: String,
  text: String,
  emoji: Boolean
)

object WebhookText {
  implicit val rw: ReadWriter[WebhookText] = macroRW
}

case class WebhookRow(
  `type`: String,
  text: String
)

object WebhookRow {
  implicit val rw: ReadWriter[WebhookRow] = macroRW
}

object SlackBlocks:

  private def textCell(value: String): Obj =
    Obj(
      "type" -> "raw_text",
      "text" -> value
    )

  def table(
      headers: Seq[String],
      rows: Seq[Seq[String]]
  ): Obj =
    require(
      rows.forall(_.length == headers.length),
      "All rows must have the same number of columns as headers"
    )

    Obj(
      "type" -> "table",
      // "column_settings" -> headers.map(textCell),
      "rows" -> {
        val a = headers.map(textCell)
        val b = rows.map(row => row.map(textCell))
        a +: b
      }
    )

  def header(text: String): Obj =
    Obj(
      "type" -> "header",
      "text" -> Obj(
        "type" -> "plain_text",
        "text" -> text,
        "emoji" -> true
      )
    )