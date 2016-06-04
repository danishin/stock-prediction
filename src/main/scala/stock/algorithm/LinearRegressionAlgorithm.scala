package stock.algorithm

import java.time.LocalDate
import javax.inject.Singleton

import org.saddle.index.InnerJoin
import org.saddle.{Frame, Index, Series}
import smile.regression.OLS
import stock.StockPredictionResult
import stock.algorithm.indicator.Indicator

case class LabeledPoint(features: Array[Double], label: Double)

/**
  *
  * @param indicators technical indicators to use for prediction. result from indicators are the X and future price is Y
  * @param numDaysForecastAhead the future window days to use for associating indicator results and future price.
  *                         optimal days to window needs to be learned through backtesting.
  */
case class LinearRegressionAlgorithmParams(indicators: Seq[Indicator], numDaysForecastAhead: Int)

class LinearRegressionModel(labeledPoints: Array[LabeledPoint]) extends Serializable {
  require(labeledPoints.nonEmpty, "Labeled Points cannot be empty")

  private val ols = new OLS(labeledPoints.map(_.features), labeledPoints.map(_.label))

  def predict(features: Array[Double]): Double =
    ols.predict(features)
}

@Singleton
class LinearRegressionAlgorithm(params: LinearRegressionAlgorithmParams) {
  import common.implicits._

  private def train(timeSeries: Series[LocalDate, Double]): LinearRegressionModel = {
    val indicatorResultsFrame: Frame[LocalDate, String, Double] = {
      val indicatorNameIndex = Index(params.indicators.map(_.name): _*)
      val indicatorResults = params.indicators.map(_.computeStandardized(timeSeries))

      Frame(indicatorResults, indicatorNameIndex)
    }

    val futurePriceSeries = timeSeries
      .shift(params.numDaysForecastAhead)
      .dropNA

    // drop indicator results at the beginning where future price is impossible.
    val labeledPoints = futurePriceSeries.joinF(indicatorResultsFrame, InnerJoin)
      .toRowSeq
      .map { case (_, series) =>
        val arr = series.values.contents
        val futurePrice = arr.head
        val indicatorResults = arr.tail

        LabeledPoint(indicatorResults, futurePrice)
      }
      .toArray

    new LinearRegressionModel(labeledPoints)
  }

  def predict(timeSeries: Series[LocalDate, Double]): StockPredictionResult = {
    val trainingTimeSeries = timeSeries.slice(0, timeSeries.length - 1)

    val model = train(trainingTimeSeries)

    val targetFeatures = params.indicators.map(_.computeStandardizedLast(timeSeries)).toArray

    val predictedPrice = model.predict(targetFeatures)

    StockPredictionResult(timeSeries.lastKey.get, predictedPrice, Map())
  }
}
