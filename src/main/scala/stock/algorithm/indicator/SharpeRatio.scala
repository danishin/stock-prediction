package stock.algorithm.indicator

import java.time.LocalDate

import org.saddle.Series

/**
  * SharpeRatio
  *
  * compute risk adjusted return
  */
class SharpeRatio extends Indicator {
  val name = "SharpeRatio"
  def compute(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    val dailyReturnSeries = stats.computeDailyReturn(priceSeries)
    // Adjustment factor for daily return sharpe ratio to convert to annualized return sharpe ratio (which is the standard)
    // This matters because Sharpe Ratio can vary widely depending on how frequently you sample.
    // 252 is the number of days for annual stock trades (i.e samples per year).
    val k = math.sqrt(252)

    // TODO: sharpe ratio is not a technical indicator. You use this for portfolio management.
    ???
  }
}

