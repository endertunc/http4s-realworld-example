package com.real.world.http4s.service

import com.real.world.http4s.AppError.{ PasswordHashFailed, UserAlreadyExist, UserNotFound }
import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.{ UpdateUserGenerator, UserGenerator, UserLoginGenerator, UserRegisterGenerator }
import com.real.world.http4s.model.profile.IsFollowing.NotFollowing
import com.real.world.http4s.model.profile.IsFollowing.Following
import com.real.world.http4s.model.user.User.PlainTextPassword
import com.real.world.http4s.model.user.UserLogin
import com.real.world.http4s.AppError.{ PasswordHashFailed, UserAlreadyExist, UserNotFound }
import com.real.world.http4s.generators.{ UpdateUserGenerator, UserGenerator, UserLoginGenerator, UserRegisterGenerator }

import cats.data.NonEmptyList
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.OptionValues

class UserServiceSpec extends AsyncFlatSpec with ServicesAndRepos with OptionValues {

  /**
    * User register tests
    */
  "UserService" should "register user when user does not exist" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      persistedUser <- ctx.userService.registerUser(userRegister)
    } yield {
      persistedUser.username shouldBe userRegister.username
      persistedUser.email shouldBe userRegister.email
    }
  }

  it should "fail with UserAlreadyExist when email already exist" in FailedIOSuit {
    val existingUserRegisterRequestIn = UserRegisterGenerator.generateUserRegister
    val newUserRegisterRequestIn      = UserRegisterGenerator.generateUserRegister.copy(email = existingUserRegisterRequestIn.email)
    for {
      _ <- ctx.userService.registerUser(existingUserRegisterRequestIn)
      _ <- ctx.userService.registerUser(newUserRegisterRequestIn)
    } yield fail("User registered with the existing email")
  }(_ shouldBe a[UserAlreadyExist])

  it should "fail with UserAlreadyExist when username already exist" in FailedIOSuit {
    val existingUserRegisterRequestIn = UserRegisterGenerator.generateUserRegister
    val newUserRegisterRequestIn      = UserRegisterGenerator.generateUserRegister.copy(username = existingUserRegisterRequestIn.username)
    for {
      _ <- ctx.userService.registerUser(existingUserRegisterRequestIn)
      _ <- ctx.userService.registerUser(newUserRegisterRequestIn)
    } yield fail("User registered with the existing username")
  }(_ shouldBe a[UserAlreadyExist])

  /**
    * User login tests
    */
  it should "login user when user already exists and has correct password" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      _ <- ctx.userService.registerUser(userRegister)
      userLogin = UserLoginGenerator.fromUserRegister(userRegister)
      loggedInUser <- ctx.userService.loginUser(userLogin)
    } yield {
      loggedInUser.username shouldBe userRegister.username
      loggedInUser.email shouldBe userRegister.email
    }
  }

  it should "fail with PasswordHashMach when password match failed for existing user" in FailedIOSuit {
    for {
      persistedUser <- insertUser()
      _             <- ctx.userService.loginUser(UserLogin(persistedUser.email, PlainTextPassword("wrong-password")))
    } yield fail("Successful login with wrong password")
  }(_ shouldBe a[PasswordHashFailed])

  it should "fail with UserNotFound when user does not exists" in FailedIOSuit {
    val userLogin = UserLoginGenerator.fromUserRegister(UserRegisterGenerator.generateUserRegister)
    for {
      _ <- ctx.userService.loginUser(userLogin)
    } yield fail("Unpersisted user found")
  }(_ shouldBe a[UserNotFound])

  /**
    * Update user tests
    */
  it should "update user when user already exist" in IOSuit {
    for {
      persistedUser <- insertUser()
      updatedUser = UpdateUserGenerator.generateUpdateUserWrapper.user
      updatedUserAfterUpdate <- ctx.userService.updateUser(updatedUser, persistedUser.id)
    } yield {
      updatedUserAfterUpdate.email shouldBe updatedUser.email.value
      updatedUserAfterUpdate.username shouldBe updatedUser.username.value
      updatedUserAfterUpdate.bio shouldBe updatedUser.bio
      updatedUserAfterUpdate.image shouldBe updatedUser.image
    }
  }

  it should "fail to update user when user does not exist" in FailedIOSuit {
    val unpersistedUser = UserGenerator.generateUser()
    val updatedUser     = UpdateUserGenerator.generateUpdateUserWrapper.user
    for {
      _ <- ctx.userService.updateUser(updatedUser, unpersistedUser.id)
    } yield fail("Non-existing user updated")
  }(_ shouldBe a[UserNotFound])

  /**
    * User findBy tests
    */
  it should "find user by username" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      _             <- ctx.userService.registerUser(userRegister)
      persistedUser <- ctx.userService.findUserByUsername(userRegister.username)
    } yield {
      persistedUser.username shouldBe userRegister.username
      persistedUser.email shouldBe userRegister.email
    }
  }

  it should "failed to find user by username when user does not exist" in FailedIOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      _ <- ctx.userService.findUserByUsername(userRegister.username)
    } yield fail("Unpersisted user found")
  }(_ shouldBe a[UserNotFound])

  it should "find user by id" in IOSuit {
    val userRegister = UserRegisterGenerator.generateUserRegister
    for {
      persistedUser <- ctx.userService.registerUser(userRegister)
      retrievedUser <- ctx.userService.findUserById(persistedUser.id)
    } yield {
      retrievedUser.username shouldBe userRegister.username
      retrievedUser.email shouldBe userRegister.email
    }
  }

  it should "failed to find user by id when user does not exist" in FailedIOSuit {
    val user = UserGenerator.generateUser
    for {
      _ <- ctx.userService.findUserById(user.id)
    } yield fail("Unpersisted user found")
  }(_ shouldBe a[UserNotFound])

  it should "list profiles with correct following status" in IOSuit {
    for {
      follower              <- insertUser()
      anotherFollower       <- insertUser()
      followeeUser1         <- insertUser()
      followeeUser2         <- insertUser()
      userWithZeroFollowers <- insertUser()
      _                     <- ctx.followerService.follow(followeeUser1.id, anotherFollower.id)
      _                     <- ctx.followerService.follow(followeeUser2.id, anotherFollower.id)
      _                     <- ctx.followerService.follow(followeeUser1.id, follower.id)
      _                     <- ctx.followerService.follow(followeeUser2.id, follower.id)
      profiles <- ctx.userService
        .findProfilesByUserId(NonEmptyList.of(followeeUser1.id, followeeUser2.id, userWithZeroFollowers.id), Some(follower.id))
    } yield {
      profiles should have size 3
      profiles(followeeUser1.id).following shouldBe Following
      profiles(followeeUser2.id).following shouldBe Following
      profiles(userWithZeroFollowers.id).following shouldBe NotFollowing
    }
  }

}
