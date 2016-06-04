package stock.algorithm.indicator

import java.time.LocalDate

import breeze.linalg.DenseVector
import org.saddle.Series

class BollingerBands(windowSize: Int = 14) extends Indicator {
  import common.implicits._

  val name = "BollingerBands"
  def compute(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    val rollingMean = priceSeries.rolling(windowSize, _.mean)
    val rollingStd = priceSeries.rolling(windowSize, _.stdev)

    val upperBand = rollingMean + (rollingStd * 2)
    val lowerBand = rollingMean - (rollingStd * 2)



    // TODO: START FROM HERE!!!! compute intersection points for each of upper and lower bands with priceSeries and mark selling point as -1, buying point as 1 and all else 0

    ???
  }
}

