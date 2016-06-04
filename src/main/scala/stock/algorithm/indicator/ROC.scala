package stock.algorithm.indicator

import java.time.LocalDate

import org.saddle.Series

/**
  * Rate of Change Indicator
  *
  * Momentum = close_today - close_n_days_ago
  * ROC = Momentum / close_n_days_ago
  *
  * The momentum and ROC indicators show trend by remaining positive while an uptrend is sustained,
  * or negative while a downtrend is sustained
  */
class ROC(momentumPeriod: Int = 14) extends Indicator {
  val name = "Momentum"
  protected def compute(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    val rocSeries = priceSeries.rolling(momentumPeriod, s => (s.first - s.last) / s.last)

    // Fill in first 14 days offset (due to `rolling`) with 50 to maintain results
    rocSeries.reindex(priceSeries.index).fillNA(_ => 0.0)
  }
}

