import java.time.{LocalDate, Period}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.{AbstractModule, Guice}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import stock.data.DataSource
import stock.{StockLearner, StockPredictionParams}

import scala.concurrent.{Await, Future}

class AppModule extends AbstractModule {
  def configure(): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    bind(classOf[WSClient]).toInstance(AhcWSClient())
  }
}

object Command extends Enumeration {
  val Predict = Value("predict")
  val Evaluate = Value("evaluate")
}
private case class Config(command: Command.Value = Command.Predict, tickers: Seq[String] = Seq())

object Application {
  import common.implicits._

  import concurrent.duration._

  private val stockPredictionParams = StockPredictionParams(Period.ofYears(5), 5)

  private lazy val learner = {
    val injector = Guice.createInjector(new AppModule)
    injector.getInstance(classOf[StockLearner])
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("stock-prediction") {
      cmd(Command.Predict.toString)
        .action { case (_, a) =>  a.copy(command = Command.Predict) }

      cmd(Command.Evaluate.toString)
        .action { case (_, a) => a.copy(command = Command.Evaluate) }

      opt[Seq[String]]("tickers")
        .required()
        .action { case (tickers, a) => a.copy(tickers = tickers) }
        .validate(tickers => if (tickers.nonEmpty) success else failure("tickers cannot be empty"))
    }

    parser.parse(args, Config()) match {
      case Some(config) =>
        config.command match {
          case Command.Predict => predict(config.tickers)
          case Command.Evaluate => evaluate(config.tickers)
        }

      case None =>
    }
  }

  private def predict(tickers: Seq[String]) = {
    val prediction = learner.predict(tickers, LocalDate.now(), stockPredictionParams)

    val result = Await.result(prediction, 20.seconds)

    println(result)
  }

  private def evaluate(tickers: Seq[String]/*, dataSource: DataSource*/) = {
    val todays = {
      /// Number of iterations for validation
      val k = 10
      val today = LocalDate.now()
      (0 until k).map(i => today.minusYears(i)).toList
    }

    val params = List(
      StockPredictionParams(Period.ofYears(5), 5),
      StockPredictionParams(Period.ofYears(5), 4),
      StockPredictionParams(Period.ofYears(5), 3),
      StockPredictionParams(Period.ofYears(5), 2),
      StockPredictionParams(Period.ofYears(5), 1)
    )

    val prediction = Future.traverse(params) { param =>
      Future.traverse(todays) { today =>
        // TODO: START FROM HERE!!! Need to get truth data. Where to place the method? StockLearner or YahooDataSource?
        // TODO: This requires reinventing a lot of prediction io's evaluation framework. you know how it's done. it's just time-consuming. come back to it when you feel like to.
        // TODO: predict part works but since evaluate hasn't been fully implemented, it currently uses sub-optimal parameters and cannot be validated.
        val predictedPrice = learner.predict(tickers, today, param)

        predictedPrice
      }
        .map(l => (param, l))
    }

    val result = Await.result(prediction, 20.seconds)

    println(result)
  }
}
