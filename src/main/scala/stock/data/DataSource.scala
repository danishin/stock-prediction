package stock.data

import java.time.LocalDate

import com.google.inject.ImplementedBy
import org.saddle.Series

import scala.concurrent.Future

case class DataSourceParams(tickers: Seq[String], startDate: LocalDate, endDate: LocalDate)

@ImplementedBy(classOf[YahooDataSource])
trait DataSource {
  val MarketTicker = "SPY"

  def getTraining(params: DataSourceParams): Future[Map[String, Series[LocalDate, Double]]]
}
