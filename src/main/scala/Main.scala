
import scala.util.Try
import ujson.{Obj, Arr}

@main def hello(): Unit =
  val url = "https://marsapi.ams.usda.gov/services/v3.1/reports"
  val reportSlugId = 3324
  val queryParams: Map[String, String] = Map(
    "lastReports" -> "1",
    "allSections" -> "true"
  )

  val usdaApiKey = Secrets.usdaApiKey

  val resp = requests.get(
    s"${url}/${reportSlugId}",
    params = queryParams,
    auth = (usdaApiKey, "")
  )

  val respJson = ujson.read(resp.data.toString)

  val respArrOpt = respJson.arrOpt

  val reportDetailsOpt = for {
    arr <- respArrOpt
    json <- arr.find: section =>
      section.obj.get("reportSection")
        .map(_.str) == Some("Report Details")
    reportDetails <- Try(upickle.read[ReportDetails](json)).toOption
  } yield reportDetails

  val avocadosOpt = reportDetailsOpt.map: reportDetails =>
    reportDetails.results.filter: commodity =>
      commodity.commodity == "Avocados"

  val avocadoPriceGroups = avocadosOpt.map: avocados =>
    avocados.groupBy(_.region).map: (region, commodities) =>
      region -> commodities.groupMap(_.size): commodity =>
        commodity.wtd_avg_price

  val avocadoPrices = avocadosOpt.map: avocados =>
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

  println(avocadoPrices)


  val webhookUrl = Secrets.slackWebhook
  // // val payload = ujson.Obj("text" -> "🥑 Hello from AvocadoBot (test webhook)!")

  avocadoPrices.foreach: payload =>
    val postResp = requests.post(
      url = webhookUrl,
      data = payload.render(),
      headers = Seq("Content-Type" -> "application/json")
    )

    println(postResp.statusCode)
    println(postResp.text())

  // val postResp = requests.post(
  //   url = webhookUrl,
  //   data = payload.render(),
  //   headers = Seq("Content-Type" -> "application/json")
  // )

  // println(postResp.statusCode)
  // println(postResp.text())
  

  // println(avocadoPriceGroups)
  

object Secrets:
  def requiredEnv(name: String): String =
    sys.env.getOrElse(
      name,
      throw new IllegalStateException(s"Missing required env var: $name")
    )

  lazy val usdaApiKey: String      = requiredEnv("USDA_MMN_API_KEY")
  lazy val slackWebhook: String    = requiredEnv("SLACK_WEBHOOK")
