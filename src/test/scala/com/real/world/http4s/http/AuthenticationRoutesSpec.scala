package com.real.world.http4s.http

import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{ Request, _ }
import cats.effect.IO
import com.real.world.http4s.RealWorldApp
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.literal._
import io.circe.refined._
import io.circe.{ Decoder, Encoder }
import com.real.world.http4s.generators.{ UserLoginGenerator, UserRegisterGenerator }
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model.user._
import com.real.world.http4s.model.{ Error, ErrorWrapperOut }
import org.scalatest.flatspec.AsyncFlatSpec
import eu.timepit.refined.auto._

class AuthenticationRoutesSpec extends AsyncFlatSpec with RealWorldApp with CirceEntityDecoder with CirceEntityEncoder {

  implicit val UserRegisterRequestInEncoder: Encoder[RegisterUserInput]               = deriveEncoder[RegisterUserInput]
  implicit val UserRegisterRequestInWrapperEncoder: Encoder[RegisterUserInputWrapper] = deriveEncoder[RegisterUserInputWrapper]
  implicit val UserLoginRequestInEncoder: Encoder[UserLoginInput]                     = deriveEncoder[UserLoginInput]
  implicit val UserLoginRequestInWrapperEncoder: Encoder[UserLoginInputWrapper]       = deriveEncoder[UserLoginInputWrapper]
  implicit val UserResponseOutDecoder: Decoder[UserResponse]                          = deriveDecoder[UserResponse]
  implicit val UserResponseOutWrapperDecoder: Decoder[UserResponseWrapper]            = deriveDecoder[UserResponseWrapper]

  implicit val ErrorDecoder: Decoder[Error]                     = deriveDecoder[Error]
  implicit val ErrorWrapperOutDecoder: Decoder[ErrorWrapperOut] = deriveDecoder[ErrorWrapperOut]

  private val apiUsers: Uri = uri"/api/users"

  "App" should "allow user to register" in {
    val userRegisterInput            = UserRegisterGenerator.generateUserRegisterInput
    val userRegisterRequestInWrapped = RegisterUserInputWrapper(userRegisterInput)
    val request = Request[IO](
      method = Method.POST,
      uri    = apiUsers,
      body   = userRegisterRequestInWrapped.toJsonBody
    )
    val response = ctx.httpApp.run(request).unsafeRunSync()

    val userResponseOutWrapper = response.as[UserResponseWrapper].unsafeRunSync
    userResponseOutWrapper.user.username.value.value should be(userRegisterInput.username)
    userResponseOutWrapper.user.email.value.value should be(userRegisterInput.email)
  }

  it should "not allow user to register with same email" in IOSuit {
    val userRegisterRequest = UserRegisterGenerator.generateUserRegisterRequest

    for {
      persistedUser <- ctx.userService.registerUser(userRegisterRequest)
      userRegister                 = UserRegisterGenerator.generateUserRegisterInput.copy(email = persistedUser.email.value.value)
      userRegisterRequestInWrapped = RegisterUserInputWrapper(userRegister)
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers,
        body   = userRegisterRequestInWrapped.toJsonBody
      )
      response        <- ctx.httpApp.run(request)
      _               <- IO(response.status shouldBe Status.Conflict)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1

  }

  it should "not allow user to register with same username" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegisterRequest

    for {
      persistedUser <- ctx.userService.registerUser(userRegister)
      userRegister                 = UserRegisterGenerator.generateUserRegisterInput.copy(username = persistedUser.username.value.value)
      userRegisterRequestInWrapped = RegisterUserInputWrapper(userRegister)
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers,
        body   = userRegisterRequestInWrapped.toJsonBody
      )
      response        <- ctx.httpApp.run(request)
      _               <- IO(response.status shouldBe Status.Conflict)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1
  }

  it should "allow existing user to login" in IOSuit {
    val userRegisterRequest = UserRegisterGenerator.generateUserRegisterRequest
    for {
      _ <- ctx.userService.registerUser(userRegisterRequest)
      userLoginRequestInWrapped = UserLoginGenerator.fromUserRegisterRequestToUserLoginInput(userRegisterRequest)
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers / "login",
        body   = UserLoginInputWrapper(userLoginRequestInWrapped).toJsonBody
      )
      response            <- ctx.httpApp.run(request)
      _                   <- IO(response.status shouldBe Status.Ok)
      userResponseWrapper <- response.as[UserResponseWrapper]
    } yield userResponseWrapper.user.email shouldBe userRegisterRequest.email
  }

  it should "failed to login with wrong password" in IOSuit {
    val userRegisterRequest = UserRegisterGenerator.generateUserRegisterRequest
    for {
      _ <- ctx.userService.registerUser(userRegisterRequest)
      userLoginRequestInWrapped = UserLoginGenerator
        .fromUserRegisterRequestToUserLoginInput(userRegisterRequest)
        .copy(password = "some-password")
      request = Request[IO](
        method = Method.POST,
        uri    = apiUsers / "login",
        body   = UserLoginInputWrapper(userLoginRequestInWrapped).toJsonBody
      )
      response        <- ctx.httpApp.run(request)
      _               <- IO(response.status shouldBe Status.BadRequest)
      errorWrapperOut <- response.as[ErrorWrapperOut]
    } yield errorWrapperOut.error.errors should have size 1
  }

  // ToDo better test or remove
//  it should "fail with json validation error when `user` key is missing" in IOSuit {
//    val request = Request(
//      method = Method.POST,
//      uri    = apiUsers,
//      body   = stringToEntityBody(json""" { } """.spaces2)
//    )
//    for {
//      response        <- ctx.httpApp.run(request)
//      _               <- IO(response.status shouldBe Status.BadRequest)
//      errorWrapperOut <- response.as[ErrorWrapperOut]
//    } yield errorWrapperOut.error.errors should have size 1
//  }

}
