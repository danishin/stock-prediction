package stock.algorithm.indicator

import java.time.LocalDate

import org.saddle.Series

/**
  * Relative Strength Index
  *
  * Indicator that implements a relative strength index formula
  *
  * RSI is a technical indicator used in the analysis of financial markets.
  * It is intended to chart the current and historical strength or weaknesses of a stock or market based on the closing prices of a recent trading period.
  *
  * RSI is classified as a momentum classifier, measuring the velocity and magnitude of directional price movements.
  * Momentum is the rate of the rise and fall in price.
  * The RSI computes momentum as the ratio of higher closes to lower closes: stocks which have had more or stronger positive changes have a higher RSI than stocks which have had more or stronger negative changes.
  *
  * The RSI is most typically used on a 14-day timeframe, measured on a scale from 0 to 100, with high and low levels marked at 70 and 30, respectively.
  *
  * RSI is a technical momentum indicator that compares the magnitude of recent gains to recent losses in an attempt to determine overbought and oversold conditions of an asset.
  *
  * @param rsiPeriod number of days to use for each of the 14 periods that are used in the RSI calculation
  */
class RSI(rsiPeriod: Int = 14, upperBound: Double = 0.7, lowerBound: Double = 0.3) extends Indicator {
  import common.implicits._

  val name = "RSI"

  protected def compute(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    val rsiSeries = getRSI(priceSeries)

    // Fill in first 14 days offset (due to `rolling`) with 50 to maintain results
    rsiSeries.reindex(priceSeries.index).fillNA(_ => 50.0)
  }

  private def getRSI(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    /**
      * RS = SMMA(U, n) / SMMA(D, n)
      *
      * where RS is Relative Strength,
      * SMMA is Modified Moving Average which is exponentially smoothed Moving Average with a = 1 / period,
      * U is Upward change where up periods are characterized by the close being higher than the previous close and
      * D is Download change down periods are characterized by the close being lower than the previous period's close.
      */
    def getRS(prices: Series[LocalDate, Double]): Series[LocalDate, Double] = {
      val dailyReturnSeries = stats.computeDailyReturn(prices)

      val upSeries = dailyReturnSeries.mapValues(dailyReturn => if (dailyReturn > 0) dailyReturn else 0)
      val downSeries = dailyReturnSeries.mapValues(dailyReturn => if (dailyReturn < 0) -dailyReturn else 0)

      val upMovingAverageSeries = upSeries.rolling(rsiPeriod, _.mean)
      val downMovingAverageSeries = downSeries.rolling(rsiPeriod, _.mean)

      upMovingAverageSeries / downMovingAverageSeries
    }

    getRS(priceSeries).mapValues(rs => 100 - (100 / (1 + rs)))
  }
}