package stock

import java.time.{LocalDate, Period}
import javax.inject.{Inject, Singleton}

import play.api.Logger
import stock.algorithm.indicator.{BollingerBands, RSI}
import stock.algorithm.{LinearRegressionAlgorithm, LinearRegressionAlgorithmParams}
import stock.data.{DataSource, DataSourceParams}

import scala.concurrent.Future

case class StockPredictionParams(trainingWindowPeriod: Period, numDaysForecastAhead: Int)

// TODO: Also add sentiment analysis for twitter and news and stuff

@Singleton
class StockLearner @Inject() (dataSource: DataSource) {
  import common.implicits._

  private implicit val logger = Logger(classOf[StockLearner])

  def predict(tickers: Seq[String], today: LocalDate, params: StockPredictionParams): Future[Map[String, Double]] = {
    val linearRegression = {
      val lrParams = LinearRegressionAlgorithmParams(Seq(new RSI/*, new BollingerBands*/), params.numDaysForecastAhead)
      new LinearRegressionAlgorithm(lrParams)
    }

    val dsParams = DataSourceParams(tickers, today.minus(params.trainingWindowPeriod), today)

    logger.trace(s"Requested Tickers: $tickers, DataSourceParams: $dsParams")

    dataSource.getTraining(dsParams)
      .map(_.mapValues(timeSeries => linearRegression.predict(timeSeries)))
  }
}
