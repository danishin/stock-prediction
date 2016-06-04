package stock.algorithm

import java.time.LocalDate

import org.saddle.Series

package object indicator {
  import common.implicits._

  object stats {
    def computeDailyReturn(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] =
      (priceSeries - priceSeries.shift(1)).fillNA(_ => 0.0)
  }
}
