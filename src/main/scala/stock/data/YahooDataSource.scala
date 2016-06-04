package stock.data

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import common.Config
import org.saddle.Series
import play.api.Logger
import play.api.libs.json.{Json, _}
import play.api.libs.ws.WSClient
import reactivemongo.api.MongoDriver
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

case class YahooDaily(adjClose: Double, close: Double, date: LocalDate, high: Double, low: Double, open: Double, symbol: String, volume: Int)
object YahooDaily {
  implicit class JsLookupResultExt(result: JsLookupResult) {
    def parse[A](f: String => A): JsResult[A] = result.validate[String].flatMap { s =>
      try JsSuccess(f(s))
      catch { case e: Throwable => JsError(e.toString) }
    }
  }

  val readYahooAPI: Reads[YahooDaily] = Reads(json => for {
    adjClose <- (json \ "Adj_Close").parse(_.toDouble)
    close <- (json \ "Close").parse(_.toDouble)
    date <- (json \ "Date").parse(LocalDate.parse)
    high <- (json \ "High").parse(_.toDouble)
    low <- (json \ "Low").parse(_.toDouble)
    open <- (json \ "Open").parse(_.toDouble)
    symbol <- (json \ "Symbol").parse(identity)
    volume <- (json \ "Volume").parse(_.toInt)
  } yield YahooDaily(adjClose, close, date, high, low, open, symbol, volume))

  implicit val formats = Json.format[YahooDaily]
}

private object YahooDailySchema {
  val Ticker = "ticker"
  val StartDate = "start_date"
  val EndDate = "end_date"
  val YahooDailies = "yahoo_dailies"
}

@Singleton
class YahooDataSource @Inject() (ws: WSClient, config: Config) extends DataSource {
  import common.implicits._
  import reactivemongo.play.json._

  private implicit val logger = Logger(classOf[YahooDataSource])
  private val db = {
    val driver = new MongoDriver
    val connection = driver.connection(config.MongoDBParsedURI)
    connection(config.MongoDBDatabaseName)
  }
  private def collection = db.collection[JSONCollection]("yahoo_daily")

  private val YahooAPIURL = "http://query.yahooapis.com/v1/public/yql"
  private val DataTablesURL = "store://datatables.org/alltableswithkeys"

  private def fetchYahooDailyMap(tickers: Seq[String], startDate: LocalDate, endDate: LocalDate): Future[Map[String, Vector[YahooDaily]]] = {
    def downloadYahooDaily(ticker: String): Future[Vector[YahooDaily]] = {
      logger.debug("Download Daily from Yahoo API")
      // This is needed as yahoo will simply return null if one requests too much at a time.
      // TODO: refactor?
      val yearlyStartEndDates = {
        var startEndDates = Vector.empty[(LocalDate, LocalDate)]
        var currStartDate = startDate
        while (currStartDate.isBefore(endDate)) {
          val currEndDate = currStartDate.plusYears(1) match {
            case date if date.isAfter(endDate) => endDate
            case date => date
          }

          startEndDates = startEndDates :+ currStartDate -> currEndDate

          currStartDate = currEndDate.plusDays(1)
        }

        startEndDates
      }

      println(yearlyStartEndDates)

      Future.traverse(yearlyStartEndDates) { case (yearlyStartDate, yearlyEndDate) =>
        implicit val reads = YahooDaily.readYahooAPI

        ws.url(YahooAPIURL)
          .withQueryString(
            "q" -> s"""
                      |SELECT * FROM yahoo.finance.historicaldata
                      |WHERE symbol = '$ticker'
                      |  AND startDate = '$yearlyStartDate'
                      |  AND endDate = '$yearlyEndDate'
            """.stripMargin,
            "format" -> "json",
            "diagnostics" -> "false",
            "callback" -> "",
            "env" -> DataTablesURL
          )
          .get()
          .flatMap(r => (r.json \ "query" \ "results" \ "quote").validateAsFuture[Vector[YahooDaily]])
      }
        .map(_.flatten)
    }

    def insertToDB(ticker: String, dailyVec: Vector[YahooDaily], startDate: LocalDate, endDate: LocalDate): Future[Unit] =
      collection
        .update(
          selector = Json.obj(
            YahooDailySchema.Ticker -> ticker,
            YahooDailySchema.StartDate -> startDate,
            YahooDailySchema.EndDate -> endDate
          ),
          update = Json.obj(
            YahooDailySchema.Ticker -> ticker,
            YahooDailySchema.StartDate -> startDate,
            YahooDailySchema.EndDate -> endDate,
            YahooDailySchema.YahooDailies -> dailyVec
          ),
          upsert = true
        )
        .debug(_.toString)
        .map(_ => ())

    Future.traverse(tickers) { ticker =>
      for {
        jsonResult <- collection
          .find(Json.obj(
            YahooDailySchema.Ticker -> ticker,
            YahooDailySchema.StartDate -> startDate,
            YahooDailySchema.EndDate -> endDate
          ))
          .projection(Json.obj(
            YahooDailySchema.YahooDailies -> 1
          ))
          .sort(Json.obj("date" -> 1))
          .one[JsObject]

        dailyVec <- jsonResult match {
          case Some(json) =>
            (json \ YahooDailySchema.YahooDailies).validateAsFuture[Vector[YahooDaily]]
              .debug(_ => "Fetched existing document!")

          case None =>
            logger.debug(s"Didn't find matching document. Try yahoo API")

            for {
              dailyVec <- downloadYahooDaily(ticker)
              _        <- insertToDB(ticker, dailyVec, startDate, endDate)
            } yield dailyVec
        }
      } yield ticker -> dailyVec
    }
      .map(_.toMap)
  }

  def getTraining(params: DataSourceParams): Future[Map[String, Series[LocalDate, Double]]] = {
    val tickers =
      if (params.tickers.contains(MarketTicker)) params.tickers
      else params.tickers :+ MarketTicker

    logger.trace(s"Tickers: $tickers")

    fetchYahooDailyMap(tickers, params.startDate, params.endDate)
      .debug(m => s"Fetched YahooDaily: ${m.mapValues(_.length)}")
      .map { yahooDailyMap =>
        val (marketDailyMap, dailyMap) = yahooDailyMap.partition(_._1 == MarketTicker)
        val marketTimeSeries = Series(marketDailyMap.head._2.map(d => d.date -> d.adjClose): _*).sortedIx

        dailyMap
          .mapValues { dailyVector =>
            val timeSeries = Series(dailyVector.map(d => d.date -> d.adjClose): _*).sortedIx

            // Align with market time series, which marks all missing values that exist on market time series as NA
            // Fill forward first
            // And then fill backwards, so that ones starting with missing data can be filled with first non-NA value
            marketTimeSeries
              .align(timeSeries)._2
              .pad
              .padReversed
          }
      }
  }
}
