package stock.algorithm.indicator

import java.time.LocalDate

import org.saddle.Series

trait Indicator {
  import common.implicits._

  def name: String

  protected def compute(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double]

  /**
    * compute indicator results and standardize the values to be between -1.0 ~ 1.0
    */
  def computeStandardized(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    val series = compute(priceSeries)

    (series - series.mean) / series.stdev
  }

  /**
    * used to compute last day's indicator results for prediction.
    * keep in mind that we cannot compute just for a single day only as indicators are only useful with context of preceding days.
    */
  def computeStandardizedLast(priceSeries: Series[LocalDate, Double]): Double =
    computeStandardized(priceSeries).last
}
