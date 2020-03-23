package com.real.world.http4s.model.profile

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model.user.User
final case class Profile(username: Username, bio: Option[Bio], image: Option[Image], following: IsFollowing)
final case class ProfileResponseOutWrapper(profile: Profile)

object Profile {
  implicit val ProfileEncoder: Encoder[Profile] = deriveEncoder[Profile]
  implicit val ProfileDecoder: Decoder[Profile] = deriveDecoder[Profile]

  def apply(user: User, isFollowing: IsFollowing): Profile = new Profile(user.username, user.bio, user.image, isFollowing)
}

object ProfileResponseOutWrapper {
  implicit val ProfileResponseOutWrapperEncoder: Encoder[ProfileResponseOutWrapper] = deriveEncoder[ProfileResponseOutWrapper]
}
