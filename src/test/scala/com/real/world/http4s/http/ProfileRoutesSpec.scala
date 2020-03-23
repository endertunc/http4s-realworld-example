package com.real.world.http4s.http

import org.http4s.Credentials.Token
import org.http4s.circe.{ CirceEntityDecoder, CirceEntityEncoder }
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.{ Headers, Method, Request }

import cats.effect.IO

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.refined._

import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model.profile.{ IsFollowing, Profile, ProfileResponseOutWrapper }

import org.scalatest.flatspec.AsyncFlatSpec

class ProfileRoutesSpec extends AsyncFlatSpec with ServicesAndRepos with CirceEntityDecoder with CirceEntityEncoder {

  implicit val ProfileDecoder: Decoder[Profile]                                     = deriveDecoder[Profile]
  implicit val ProfileResponseOutWrapperDecoder: Decoder[ProfileResponseOutWrapper] = deriveDecoder[ProfileResponseOutWrapper]

  private val apiProfiles = uri"/api/profiles"

  "Profile routes" should "return user profile with following(false) information" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      jwt               <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.GET,
        uri     = apiProfiles / persistedFollowee.username.value,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt.value)))
      )
      response                  <- ctx.httpApp.run(request)
      profileResponseOutWrapper <- response.as[ProfileResponseOutWrapper]
    } yield profileResponseOutWrapper.profile.following shouldBe IsFollowing.NotFollowing
  }

  it should "return user profile with following (true) information " in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.profileService.follow(persistedFollowee.username, persistedUser.id)
      jwt               <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.GET,
        uri     = apiProfiles / persistedFollowee.username.value,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt.value)))
      )
      response                  <- ctx.httpApp.run(request)
      profileResponseOutWrapper <- response.as[ProfileResponseOutWrapper]
    } yield profileResponseOutWrapper.profile.following shouldBe IsFollowing.Following
  }

  it should "allow user to follow other users " in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      jwt               <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.POST,
        uri     = apiProfiles / s"${persistedFollowee.username.value}" / "follow",
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt.value)))
      )
      response                  <- ctx.httpApp.run(request)
      profileResponseOutWrapper <- response.as[ProfileResponseOutWrapper]
    } yield profileResponseOutWrapper.profile.following shouldBe IsFollowing.Following
  }

  it should "allow user to unfollow already followed users" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.profileService.follow(persistedFollowee.username, persistedUser.id)
      jwt               <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.DELETE,
        uri     = apiProfiles / s"${persistedFollowee.username.value}" / "follow",
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt.value)))
      )
      response                  <- ctx.httpApp.run(request)
      profileResponseOutWrapper <- response.as[ProfileResponseOutWrapper]
      _ = profileResponseOutWrapper.profile.following shouldBe IsFollowing.NotFollowing
      isFollowing <- ctx.followerService.isFollowing(persistedUser.id, persistedFollowee.id)
    } yield isFollowing shouldBe IsFollowing.NotFollowing
  }

}
