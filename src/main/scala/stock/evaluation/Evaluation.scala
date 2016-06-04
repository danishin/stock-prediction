package stock.evaluation

import java.time.LocalDate

import org.saddle.{Frame, Series, Vec}

case class EvaluationParams(buyThreshold: Double, sellThreshold: Double, budget: Double)
case class EvaluationResult(sharpeRatio: Double)

sealed trait StockAction
object StockAction {
  case object Buy extends StockAction
  case object Sell extends StockAction
  case object Hold extends StockAction
}

class Evaluation(params: EvaluationParams) {
  import common.implicits._

  import breeze.linalg._

  private def computeDailyPortfolioVals(priceSeriesMap: Map[String, Series[LocalDate, Double]], allocRatio: Seq[Double], budget: Double): Series[LocalDate, Double] = {
    require(priceSeriesMap.size == allocRatio.size, "Price Series Map and Allocation Ratio must be of equal size")
    require(allocRatio.sum == 1.0, "Sum of each stock's ratio in portfolio must add up to 1.0")

    val pricesM = DenseMatrix(priceSeriesMap.toArray.map(_._2.values.contents): _*)

    val normalizedM = pricesM(::, *) :/ pricesM(::, 0)

    val allocatedM = normalizedM(::, *) :* DenseVector(allocRatio: _*)

    val positionValsM = allocatedM :* budget

    val portfolioValsM = sum(positionValsM, Axis._1)

    Series(portfolioValsM.toSaddleVec, priceSeriesMap.head._2.index)
  }

  private def computeSharpeRatio(priceSeries: Series[LocalDate, Double], riskFreeReturn: Double = 0.0): Double = {
    val dailyRetS = priceSeries / priceSeries.shift(1) - 1

    // Adjustment factor for daily return sharpe ratio to convert to annualized return sharpe ratio
    // 252 is the number of days for annual stock trades.
    val k = math.sqrt(252)

    val sharpeRatio = (dailyRetS.mean - riskFreeReturn) / dailyRetS.stdev

    val annualizedSharpeRatio = sharpeRatio * k

    annualizedSharpeRatio
  }

  def backtest(indicatorResultsSeries: Series[LocalDate, Double]): EvaluationResult = {


    // TODO: START FROM HERE!!!!!! Compute sharpe ratio
    indicatorResultsSeries.toSeq.foreach { a =>

      ???
    }


    ???
  }
}
