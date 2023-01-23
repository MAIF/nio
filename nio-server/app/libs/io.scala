package libs

import cats.Parallel
import cats.data.EitherT
import cats.implicits._
import cats.syntax._

import scala.concurrent.{ExecutionContext, Future}

object io {

  type IO[E, A] = EitherT[Future, E, A]

  implicit class IOSyntax[E, A](value: EitherT[Future, E, A]) {
    def mapError[E1](func: E => E1)(implicit ec: ExecutionContext): IO[E1, A] = value.leftMap(func)

    def doOnError(func: E => Unit)(implicit ec: ExecutionContext): IO[E, A] = {
      value.leftMap { e =>
        func(e)
        e
      }
    }

    def keep(predicate: A => Boolean, ifFalse: => E)(implicit ec: ExecutionContext): IO[E, A] = {
      value.flatMap { a =>
        if (predicate(a)) {
          EitherT.rightT[Future, E](a)
        } else {
          EitherT.leftT[Future, A](ifFalse)
        }
      }
    }
    def keep(predicate: A => Boolean, ifFalse: A => E)(implicit ec: ExecutionContext): IO[E, A] = {
      value.flatMap { a =>
        if (predicate(a)) {
          EitherT.rightT[Future, E](a)
        } else {
          EitherT.leftT[Future, A](ifFalse(a))
        }
      }
    }

    def fold[B](onError: E => Future[B], onSuccess: A => Future[B])(implicit ec: ExecutionContext) : Future[B] = {
      value.value.flatMap { either =>
        either.fold(onError, onSuccess)
      }
    }
  }

  implicit class FutureOps[A](value: Future[A]) {
    def io[E](implicit executor: ExecutionContext): IO[E, A] = {
      IO.fromFuture[E](value)
    }
  }

  implicit class FutureEitherOps[E, A](value: Future[Either[E, A]]) {
    def io(implicit executor: ExecutionContext): IO[E, A] = {
      IO(value)
    }
  }

  implicit class IterableOps[A](value: List[A]) {
    def traverse[E, B](func: A => IO[E, B])(implicit executor: ExecutionContext): IO[Seq[E], List[B]] = {
      IO.fromFuture(Future.traverse(value) { a =>
        func(a).value
      }).flatMap { res =>
        val errors = res.collect { case Left(l) => l }
        if (errors.isEmpty) {
          IO.succeed(res.collect { case Right(r) => r })
        } else {
          IO.error(errors)
        }
      }
    }
  }

  object IO {

    import cats.implicits._
    def apply[E, A](underlying: Future[Either[E, A]]): IO[E, A] = EitherT(underlying)

    final private[io] case class PartialSucceedApply[E](private val dummy: Boolean = true) extends AnyVal {
      def apply[A](value: A)(implicit ec: ExecutionContext): IO[E, A] = EitherT.pure[Future, E](value)
    }
    def succeed[E]: PartialSucceedApply[E] = new PartialSucceedApply[E]

    final private[io] case class PartialFromFutureApply[E](private val dummy: Boolean = true) extends AnyVal {
      def apply[A](value: Future[A])(implicit ec: ExecutionContext): IO[E, A] = EitherT.right[E](value)
    }

    def fromFuture[E]: PartialFromFutureApply[E] = new PartialFromFutureApply[E]
    def fromFutureOption[E, A](value: Future[Option[A]], ifNone: => E)(implicit ec: ExecutionContext): IO[E, A] =
      EitherT.fromOptionF(value, ifNone)

    def error[E, A](value: E)(implicit ec: ExecutionContext): IO[E, A] = EitherT.leftT[Future, A](value)
    def fromEither[E, A](value: Either[E, A])(implicit ec: ExecutionContext): IO[E, A] = EitherT.fromEither(value)
    def fromOption[A](value: Option[A])(implicit ec: ExecutionContext): IO[Unit, A] = EitherT.fromOption(value, ())
    def fromOption[E, A](value: Option[A], ifNone: => E)(implicit ec: ExecutionContext): IO[E, A] = EitherT.fromOption(value, ifNone)



  }

}
