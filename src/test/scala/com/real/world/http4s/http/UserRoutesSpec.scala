package com.real.world.http4s.http

import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.circe.{ CirceEntityDecoder, CirceEntityEncoder }
import org.http4s.headers.Authorization
import org.http4s.implicits._
import cats.effect.IO
import com.real.world.http4s.RealWorldApp
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.refined._
import io.circe.{ Decoder, Encoder }
import com.real.world.http4s.generators.UpdateUserGenerator
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model.user._
import org.scalatest.flatspec.AsyncFlatSpec

class UserRoutesSpec extends AsyncFlatSpec with RealWorldApp with CirceEntityDecoder with CirceEntityEncoder {

  implicit val UserUpdateRequestInEncoder: Encoder[UpdateUser]               = deriveEncoder[UpdateUser]
  implicit val UpdateUserRequestInWrapperEncoder: Encoder[UpdateUserWrapper] = deriveEncoder[UpdateUserWrapper]

  implicit val UserResponseOutDecoder: Decoder[UserResponse]               = deriveDecoder[UserResponse]
  implicit val UserResponseOutWrapperDecoder: Decoder[UserResponseWrapper] = deriveDecoder[UserResponseWrapper]

  private val apiUser: Uri = uri"/api/user"

  "App" should "return current user" in IOSuit {
    for {
      persistedUser <- insertUser()
      jwt           <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.GET,
        uri     = apiUser,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt.value.value)))
      )
      response               <- ctx.httpApp.run(request)
      userResponseOutWrapper <- response.as[UserResponseWrapper]
    } yield {
      userResponseOutWrapper.user.email shouldBe persistedUser.email
      userResponseOutWrapper.user.username shouldBe persistedUser.username
    }
  }

  it should "allow user to update" in IOSuit {
    val updateUserRequest: UpdateUserWrapper = UpdateUserGenerator.generateUpdateUserWrapper
    for {
      persistedUser <- insertUser()
      jwt           <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.PUT,
        uri     = apiUser,
        body    = updateUserRequest.toJsonBody,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt.value.value)))
      )
      response               <- ctx.httpApp.run(request)
      userResponseOutWrapper <- response.as[UserResponseWrapper]
    } yield {
      val userResponseOut = updateUserRequest.user
      Some(userResponseOutWrapper.user.email) shouldBe userResponseOut.email
      Some(userResponseOutWrapper.user.username) shouldBe userResponseOut.username
      userResponseOutWrapper.user.bio shouldBe userResponseOut.bio
      userResponseOutWrapper.user.image shouldBe userResponseOut.image
    }
  }

}
