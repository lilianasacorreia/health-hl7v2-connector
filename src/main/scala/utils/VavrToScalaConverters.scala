package utils

import io.vavr.collection.List as VavrList
import io.vavr.concurrent.Future as VavrFuture
import io.vavr.control.{Either as VavrEither, Option as VavrOption, Try as VavrTry}

import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.util.{Failure, Success, Try}

/**
 * Utility object providing bi-directional conversion methods between
 * Scala standard library types and Vavr functional types.
 *
 * <p>Includes conversion for:</p>
 * <ul>
 * <li>{@link io.vavr.control.Option} ↔ {@link scala.Option}</li>
 * <li>{@link io.vavr.control.Either} ↔ {@link scala.util.Either}</li>
 * <li>{@link io.vavr.control.Try} ↔ {@link scala.util.Try}</li>
 * <li>{@link io.vavr.collection.List} ↔ {@link scala.collection.immutable.List}</li>
 * <li>{@link io.vavr.concurrent.Future} → {@link scala.concurrent.Future}</li>
 * </ul>
 *
 */
object VavrToScalaConverters:

  /** Converts a Vavr {@link io.vavr.control.Option} into a Scala {@link scala.Option}. */
  extension [T](vOpt: VavrOption[T])
    inline def asScala: Option[T] =
      if vOpt.isDefined then Some(vOpt.get)
      else None

  /** Converts a Scala {@link scala.Option} into a Vavr {@link io.vavr.control.Option}. */
  extension [T](option: Option[T])
    inline def asVavrOption: VavrOption[T] =
      option.map(VavrOption.of[T]).getOrElse(VavrOption.none[T]())

  /** Converts a Vavr {@link io.vavr.control.Either} into a Scala {@link scala.util.Either}. */
  extension [L, R](vEither: VavrEither[L, R])
    inline def asScala: Either[L, R] =
      if vEither.isLeft then Left(vEither.getLeft)
      else Right(vEither.get)

  /** Converts a Scala {@link scala.util.Either} into a Vavr {@link io.vavr.control.Either}. */
  extension [L, R](either: Either[L, R])
    inline def asVavrEither: VavrEither[L, R] =
      either match
        case Left(l)  => VavrEither.left(l)
        case Right(r) => VavrEither.right(r)

  /** Converts a Vavr {@link io.vavr.control.Try} into a Scala {@link scala.util.Try}. */
  extension [T](vTry: VavrTry[T])
    inline def asScala: Try[T] =
      if vTry.isSuccess then Success(vTry.get)
      else Failure(vTry.getCause)

  /** Converts a Scala {@link scala.util.Try} into a Vavr {@link io.vavr.control.Try}. */
  extension [T](sTry: Try[T])
    inline def asVavrTry: VavrTry[T] =
      sTry match
        case Success(x)       => VavrTry.success(x)
        case Failure(throwable) => VavrTry.failure(throwable)

  /**
   * Converts a Vavr {@link io.vavr.collection.List} into a Scala immutable {@link scala.collection.immutable.List}.
   *
   * <p>This uses {@code toJavaList()} internally, which produces a standard {@code java.util.List}
   * compatible with Scala collections.</p>
   *
   * @param vavrList the Vavr List to convert
   * @tparam T the element type
   * @return a Scala immutable List containing the same elements
   */
  extension [T](vavrList: VavrList[T])
    inline def asScala: List[T] =
      vavrList.toJavaList.asScala.toList

  /**
   * Converts a Scala immutable {@link scala.collection.immutable.List} into a Vavr {@link io.vavr.collection.List}.
   *
   * @param list the Scala List to convert
   * @tparam T the element type
   * @return a Vavr List containing the same elements
   */
  extension [T](list: List[T])
    inline def asVavrList: VavrList[T] =
      import scala.jdk.CollectionConverters.*
      VavrList.ofAll(list.asJava)

  /**
   * Converts a Vavr {@link io.vavr.concurrent.Future} into a Scala {@link scala.concurrent.Future}.
   *
   * <p>This conversion wraps the underlying {@code CompletableFuture}
   * exposed by Vavr's {@code Future} implementation.</p>
   *
   * @param vavrFuture the Vavr Future to convert
   * @tparam T result type
   * @return a Scala Future representing the same asynchronous computation
   */
  extension [T](vavrFuture: VavrFuture[T])
    inline def asVavrToScalaFuture: scala.concurrent.Future[T] =
      vavrFuture.toCompletableFuture.asScala

end VavrToScalaConverters
