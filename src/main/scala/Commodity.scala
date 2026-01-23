import upickle.{ReadWriter, macroRW}

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
  wtd_avg_price: Double
)

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