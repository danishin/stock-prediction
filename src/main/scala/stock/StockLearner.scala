package stock

import java.time.{LocalDate, Period}
import javax.inject.{Inject, Singleton}

import play.api.Logger
import stock.algorithm.indicator.{BollingerBands, RSI}
import stock.algorithm.{LinearRegressionAlgorithm, LinearRegressionAlgorithmParams}
import stock.data.{DataSource, DataSourceParams}

import scala.concurrent.Future

case class StockPredictionResult(predictedDate: LocalDate, price: Double, indicatorResults: Map[String, Double])

// TODO: Also add sentiment analysis for twitter and news and stuff

@Singleton
class StockLearner @Inject()(dataSource: DataSource) {
  import common.implicits._

  private implicit val logger = Logger(classOf[StockLearner])

  // TODO: This needs to be exported to json or sth so that we can optimize using evaluate on the fly without touching source code.
  private val TrainingWindowPeriod = Period.ofYears(5)
  private val NumDaysForecastAhead = 5

  private val linearRegression = {
    val params = LinearRegressionAlgorithmParams(Seq(new RSI/*, new BollingerBands*/), NumDaysForecastAhead)
    new LinearRegressionAlgorithm(params)
  }

  // TODO: Currently defaults to only predicting 5 days from today.
  def predict(tickers: Array[String]): Future[Map[String, StockPredictionResult]] = {
    val today = LocalDate.now()

    val params = DataSourceParams(tickers, today.minus(TrainingWindowPeriod), today)

    logger.trace(s"Requested Tickers: $tickers, DataSourceParams: $params")

    dataSource.getTraining(params)
      .map(_.mapValues(timeSeries => linearRegression.predict(timeSeries)))
  }

  def evaluate() = {

  }
}
