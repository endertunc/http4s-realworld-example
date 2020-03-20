package com.real.world.http4s.service

import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.TagGenerator
import com.real.world.http4s.quill.{ Articles, Tags }
import com.real.world.http4s.generators.TagGenerator

import cats.data.NonEmptyList
import org.scalatest.flatspec.AsyncFlatSpec

class TagServiceSpec extends AsyncFlatSpec with ServicesAndRepos {

  "Tag Service" should "insert list of tags" in IOSuit {
    for {
      createdTags <- ctx.tagService.createTags(List(TagGenerator.generateTagIn()))
    } yield createdTags should have size 1
  }

  it should "insert missing tags and return all tags with ids" in IOSuit {
    val existingTag = TagGenerator.generateTagIn()
    for {
      _           <- ctx.tagService.createTags(List(existingTag))
      createdTags <- ctx.tagService.createTags(List(TagGenerator.generateTagIn(), existingTag))
    } yield createdTags should have size 2
  }

  it should "list all tags" in IOSuit {
    val tagIn1 = TagGenerator.generateTagIn()
    val tagIn2 = TagGenerator.generateTagIn()
    val tagIns = List(tagIn1, tagIn2)
    for {
      _       <- ctx.tagService.createTags(tagIns)
      allTags <- ctx.tagService.findAll
    } yield allTags.filter(t => tagIns.exists(p => p.name == t.name.value)) should have size 2
  }

  it should "find tags by article id" in IOSuit {
    for {
      persistedUser                     <- insertUser()
      (persistedArticle, tagsToDiscard) <- insertArticle(persistedUser.id)
      tags                              <- ctx.tagService.createTags(List(TagGenerator.generateTagIn(), TagGenerator.generateTagIn()))
      _                                 <- ctx.tagService.createArticleTagAssociation(persistedArticle.id, tags)
      tagsByArticleId                   <- ctx.tagService.findTagsByArticleId(persistedArticle.id)
    } yield tagsByArticleId.diff(tagsToDiscard) should have size 2
  }

  it should "create article-tag association" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      tags                  <- ctx.tagService.createTags(List(TagGenerator.generateTagIn()))
      _                     <- ctx.tagService.createArticleTagAssociation(persistedArticle.id, tags)
      articleTagAssociation <- Tags.findByArticleIdAndTagID(persistedArticle.id, tags.head.id)
    } yield articleTagAssociation shouldBe defined
  }

  it should "find tags by articleIds" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser.id)
      persistedArticle3 <- Articles.insertArticle(persistedUser.id)
      sharedTags        <- ctx.tagService.createTags(TagGenerator.generateTagInList(size = 2))
      tagsForArticle1   <- ctx.tagService.createTags(TagGenerator.generateTagInList(size = 3))
      tagsForArticle2   <- ctx.tagService.createTags(TagGenerator.generateTagInList(size = 1))
      _                 <- ctx.tagService.createArticleTagAssociation(persistedArticle1.id, sharedTags ++ tagsForArticle1)
      _                 <- ctx.tagService.createArticleTagAssociation(persistedArticle2.id, sharedTags ++ tagsForArticle2)
      articleTagsMap    <- ctx.tagService.findTagsByArticleIds(NonEmptyList.of(persistedArticle1.id, persistedArticle2.id, persistedArticle3.id))
    } yield {
      articleTagsMap should have size 2
      articleTagsMap(persistedArticle1.id) should have size (sharedTags ++ tagsForArticle1).size
      articleTagsMap(persistedArticle2.id) should have size (sharedTags ++ tagsForArticle2).size
      articleTagsMap.get(persistedArticle3.id) should not be defined
    }
  }

}
