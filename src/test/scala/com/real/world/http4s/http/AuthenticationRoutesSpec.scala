package com.real.world.http4s.http

import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.{ UserLoginGenerator, UserRegisterGenerator }
import com.real.world.http4s.model.user.User.PlainTextPassword
import com.real.world.http4s.model.{ Error, ErrorWrapperOut }
import com.real.world.http4s.model.user._
import org.scalatest.flatspec.AsyncFlatSpec

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.literal._
import io.circe.{ Decoder, Encoder }

import cats.effect.IO

import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{ Request, _ }

class AuthenticationRoutesSpec extends AsyncFlatSpec with ServicesAndRepos with CirceEntityDecoder with CirceEntityEncoder {

  implicit val UserRegisterRequestInEncoder: Encoder[RegisterUser]               = deriveEncoder[RegisterUser]
  implicit val UserRegisterRequestInWrapperEncoder: Encoder[RegisterUserWrapper] = deriveEncoder[RegisterUserWrapper]
  implicit val UserLoginRequestInEncoder: Encoder[UserLogin]                     = deriveEncoder[UserLogin]
  implicit val UserLoginRequestInWrapperEncoder: Encoder[UserLoginWrapper]       = deriveEncoder[UserLoginWrapper]
  implicit val UserResponseOutDecoder: Decoder[UserResponse]                     = deriveDecoder[UserResponse]
  implicit val UserResponseOutWrapperDecoder: Decoder[UserResponseWrapper]       = deriveDecoder[UserResponseWrapper]

  implicit val ErrorDecoder: Decoder[Error]                     = deriveDecoder[Error]
  implicit val ErrorWrapperOutDecoder: Decoder[ErrorWrapperOut] = deriveDecoder[ErrorWrapperOut]

  private val apiUsers: Uri = uri"/api/users"

  "App" should "allow user to register" in {
    val userRegister                 = UserRegisterGenerator.generateUserRegister
    val userRegisterRequestInWrapped = RegisterUserWrapper(userRegister)
    val request = Request[IO](
      method = Method.POST,
      uri    = apiUsers,
      body   = userRegisterRequestInWrapped.toJsonBody
    )
    val response = ctx.endpoints.run(request).unsafeRunSync()

    val userResponseOutWrapper = response.as[UserResponseWrapper].unsafeRunSync
    userResponseOutWrapper.user.username should be(userRegister.username)
    userResponseOutWrapper.user.email should be(userRegister.email)
  }

  it should "not allow user to register with same email" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister

    for {
      persistedUser <- ctx.userService.registerUser(userRegister)
      userRegister                 = UserRegisterGenerator.generateUserRegister.copy(email = persistedUser.email)
      userRegisterRequestInWrapped = RegisterUserWrapper(userRegister)
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers,
        body   = userRegisterRequestInWrapped.toJsonBody
      )
      response        <- ctx.endpoints.run(request)
      _               <- IO(response.status shouldBe Status.Conflict)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1

  }

  it should "not allow user to register with same username" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister

    for {
      persistedUser <- ctx.userService.registerUser(userRegister)
      userRegister                 = UserRegisterGenerator.generateUserRegister.copy(username = persistedUser.username)
      userRegisterRequestInWrapped = RegisterUserWrapper(userRegister)
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers,
        body   = userRegisterRequestInWrapped.toJsonBody
      )
      response        <- ctx.endpoints.run(request)
      _               <- IO(response.status shouldBe Status.Conflict)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1
  }

  it should "allow existing user to login" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      _ <- ctx.userService.registerUser(userRegister)
      userLoginRequestInWrapped = UserLoginGenerator.fromUserRegister(userRegister)
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers / "login",
        body   = UserLoginWrapper(userLoginRequestInWrapped).toJsonBody
      )
      response            <- ctx.endpoints.run(request)
      _                   <- IO(response.status shouldBe Status.Ok)
      userResponseWrapper <- response.as[UserResponseWrapper]
    } yield userResponseWrapper.user.email shouldBe userRegister.email
  }

  it should "failed to login with wrong password" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      _ <- ctx.userService.registerUser(userRegister)
      userLoginRequestInWrapped = UserLoginGenerator.fromUserRegister(userRegister).copy(password = PlainTextPassword("some-password"))
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers / "login",
        body   = UserLoginWrapper(userLoginRequestInWrapped).toJsonBody
      )
      response        <- ctx.endpoints.run(request)
      _               <- IO(response.status shouldBe Status.BadRequest)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1
  }

  it should "fail with json validation error when `user` key is missing" in IOSuit {
    val request = Request(
      method = Method.POST,
      uri    = apiUsers,
      body   = stringToEntityBody(json""" { } """.spaces2)
    )
    for {
      response        <- ctx.endpoints.run(request)
      _               <- IO(response.status shouldBe Status.BadRequest)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1
  }

}
