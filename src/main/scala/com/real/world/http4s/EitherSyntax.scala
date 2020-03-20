//package com.real.world.http4s
//
//import cats.{ Applicative, Functor }
//import cats.data.EitherT
//
//trait EitherSyntax[F[_]] {
//
//  def lift[A](a: A)(implicit F: Applicative[F]): EitherT[F, AppError, A]                = EitherT.rightT[F, AppError](a)
//  def liftF[A](a: F[A])(implicit F: Functor[F]): EitherT[F, AppError, A]                = EitherT.right[AppError](a)
//  def liftLeft[B](a: AppError)(implicit F: Applicative[F]): EitherT[F, AppError, B]     = EitherT.leftT[F, B](a)
//  def liftLeftF[B](a: F[AppError])(implicit F: Applicative[F]): EitherT[F, AppError, B] = EitherT.left(a)
//
//}
