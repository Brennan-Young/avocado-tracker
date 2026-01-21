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
  size: String,
  wtd_avg_price: Double
)

object Commodity:
  implicit val rw: ReadWriter[Commodity] = macroRW