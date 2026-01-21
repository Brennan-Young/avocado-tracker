
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

  val reportDetails = respArrOpt.map: arr =>
    arr.filter: section =>
      section.obj.get("reportSection")
        .map(_.str) == Some("Report Details")
    .headOption
  .flatten
  

  println(reportDetails)
  


object Secrets:
  def requiredEnv(name: String): String =
    sys.env.getOrElse(
      name,
      throw new IllegalStateException(s"Missing required env var: $name")
    )

  lazy val usdaApiKey: String      = requiredEnv("USDA_MMN_API_KEY")
