
import scala.util.Try
import ujson.{Obj, Arr}
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@main def hello(): Unit =
  val url = "https://marsapi.ams.usda.gov/services/v3.1/reports"
  val reportSlugId = 3324
  val queryParams: Map[String, String] = Map(
    "lastReports" -> "2",
    "allSections" -> "true"
  )

  val usdaApiKey = Secrets.usdaApiKey
  val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")

  val resp = requests.get(
    s"${url}/${reportSlugId}",
    params = queryParams,
    auth = (usdaApiKey, "")
  )

  val respJson = ujson.read(resp.data.toString)

  val respArrOpt = respJson.arrOpt

  // print(respArrOpt.get.apply(0))

  val reportEndDate = for {
    arr <- respArrOpt
    json <- arr.find: section =>
      section.obj.get("reportSection")
        .map(_.str) == Some("Report Header")
    reportHeader <- Try(upickle.read[ReportHeader](json)).toOption
    header <- reportHeader.results.headOption
  } yield {
    LocalDate.parse(header.report_end_date, dateFormat)
  }

  val reportDetailsOpt = for {
    endDate <- reportEndDate
    arr <- respArrOpt
    json <- arr.find: section =>
      section.obj.get("reportSection")
        .map(_.str) == Some("Report Details")
    reportDetails <- Try(upickle.read[ReportDetails](json)).toOption
  } yield (endDate, reportDetails)

  val avocadosOpt = reportDetailsOpt.map: (endDate, reportDetails) =>
    endDate -> reportDetails.results.filter: commodity =>
      commodity.commodity == "Avocados"

  val avocadoGroups = avocadosOpt.map: (endDate, avocados) =>
    avocados.filter: commodity =>
      (commodity.region == "Northeast" || commodity.region == "National") && commodity.size == "each"
    .groupBy: a =>
      (a.region, a.size, a.variety, a.organic)
    .flatMap: (group, avocados) =>
      val sortedAvocados = avocados.sortBy(a => LocalDate.parse(a.report_end_date, dateFormat))(using Ordering[LocalDate].reverse)

      val firstAvocado = sortedAvocados.headOption
      firstAvocado.flatMap: avocado =>
        val date = LocalDate.parse(avocado.report_end_date, dateFormat)

        date == endDate match
          case true => Some(AvocadoPrice(
            avocado.region,
            avocado.variety,
            avocado.organic,
            avocado.wtd_avg_price,
            if (sortedAvocados.size >= 2) Some(sortedAvocados(1).wtd_avg_price) else None
          ))
          case false => None

  val deltaOrdering: Ordering[Option[BigDecimal]] =
    Ordering.fromLessThan {
      case (Some(a), Some(b)) => a < b        // normal numeric ordering
      case (Some(_), None)    => true         // Some before None
      case (None, Some(_))    => false
      case (None, None)       => false
    }

  val sortedAvocadoPrices = avocadoGroups.map: avocados =>
    avocados.toSeq.sortBy(a => a.priceChange)(using deltaOrdering)
          
  val payload = sortedAvocadoPrices.map: avocadoPrices =>
    val avocadoStrings = avocadoPrices.map(avocado => avocado.asSeq)
    
    val table = SlackBlocks.table(
      Seq("Region", "Variety", "Organic?", "Price", "Last Price", "Change"),
      avocadoStrings
    )

    Obj(
      "text" -> "Avocado Prices",
      "blocks" -> Arr(
        SlackBlocks.header("Avocado Prices"),
        table
      )
    )
   

  // val avocadoPriceGroups = avocadosOpt.map: avocados =>
  //   avocados.groupBy(_.region).map: (region, commodities) =>
  //     region -> commodities.groupMap(_.size): commodity =>
  //       commodity.wtd_avg_price

  val avocadoPrices = avocadosOpt.map: (endDate, avocados) =>
    val avocadoStrings = avocados.filter: commodity =>
      (commodity.region == "Northeast" || commodity.region == "National") && commodity.size == "each"
    .map: commodity =>
      commodity.asSeq
    .sorted
    
    val table = SlackBlocks.table(
      Seq("Region", "Variety", "Organic?", "Price"),
      avocadoStrings
    )

    val payload =
      Obj(
        "text" -> "Avocado Prices",
        "blocks" -> Arr(
          SlackBlocks.header("Avocado Prices"),
          table
        )
      )
    
    payload


  val webhookUrl = Secrets.slackWebhook

  payload.foreach: p =>
    val postResp = requests.post(
      url = webhookUrl,
      data = p.render(),
      headers = Seq("Content-Type" -> "application/json")
    )

    println(postResp.statusCode)
    println(postResp.text())
  
object Secrets:
  def requiredEnv(name: String): String =
    sys.env.getOrElse(
      name,
      throw new IllegalStateException(s"Missing required env var: $name")
    )

  lazy val usdaApiKey: String      = requiredEnv("USDA_MMN_API_KEY")
  lazy val slackWebhook: String    = requiredEnv("SLACK_WEBHOOK_URL")
