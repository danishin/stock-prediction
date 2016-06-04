package common

import java.time.LocalDate

import breeze.linalg.DenseVector
import org.saddle.{Series, Vec}
import play.api.Logger
import play.api.libs.json.{JsLookupResult, Reads}

import scala.concurrent.Future
import scala.reflect.ClassTag

sealed trait StdLibImplicits {
  implicit val executionContext = concurrent.ExecutionContext.Implicits.global

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  implicit class MapExt[K, V](thisMap: Map[K, V]) {
    /**
      * do cartesian product with self for values only.
      */
    def cartesian(anotherMap: Map[K, V]): Map[(K, K), (V, V)] = {
      for {
        (k1, v1) <- thisMap
        (k2, v2) <- anotherMap
      } yield ((k1, k2), (v1, v2))
    }

    /**
      * do inner join with another map.
      */
    def innerjoin[A](anotherMap: Map[K, V])(f: (K, (V, V)) => A): Map[K, A] = {
      for {
        (k1, v1) <- thisMap
        (k2, v2) <- anotherMap if k1 == k2
      } yield (k1, f(k1, (v1, v2)))
    }
  }

  implicit class FutureExt[A](future: Future[A]) {
    def debug(f: A => String)(implicit logger: Logger): Future[A] =
      future.map { a => logger.debug(f(a)); a }
  }
}

sealed trait PlayJsonImplicits {
  implicit class JsLookupResultExt(lookupResult: JsLookupResult) {
    def validateAsFuture[A : Reads]: Future[A] =
      lookupResult.validate.asEither.fold(e => Future.failed(new RuntimeException(e.toString())), Future.successful)
  }
}

sealed trait BreezeImplicits {
  implicit class BreezeDenseVectorExt[A : ClassTag](dv: DenseVector[A]) {
    def toSaddleVec: Vec[A] = Vec(dv.toArray)
  }
}

sealed trait SaddleImplicits {
  implicit class SaddleSeriesExt[K, V](series: Series[K, V]) {
    /**
      * Same as `pad` but backwards.
      * A series of NA values is filled with the first encountered non-NA value.
      */
    def padReversed: Series[K, V] =
      series.reversed.pad.reversed
  }
}

object implicits
  extends StdLibImplicits
  with PlayJsonImplicits
  with BreezeImplicits
  with SaddleImplicits
