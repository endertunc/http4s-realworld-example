package com.real.world.http4s.repository

import cats.data.NonEmptyList
import cats.effect.IO
import com.real.world.http4s.RealWorldApp
import doobie.scalatest.IOChecker
import com.real.world.http4s.generators.ArticleGenerator
import org.scalatest.flatspec.AnyFlatSpec

class ArticlesRepositorySpec extends AnyFlatSpec with IOChecker with RealWorldApp {

  private val article                            = ArticleGenerator.generateArticle
  override def transactor: doobie.Transactor[IO] = xa

  "ArticlesStatement" should "compile" in {
    //    ArticlesStatement.listArticles(???, ???, ???, ???)

    check(ArticlesStatement.findBySlug(article.slug))
    check(ArticlesStatement.favorite(article.id, article.authorId))
    check(ArticlesStatement.unfavorite(article.id, article.authorId))
    check(ArticlesStatement.isFavoritedByUser(article.id, article.authorId))
    check(ArticlesStatement.createArticle[IO](article).unsafeRunSync)
    check(ArticlesStatement.updateArticle[IO](article).unsafeRunSync)
    check(ArticlesStatement.deleteByArticleId(article.id))
    check(ArticlesStatement.findArticleFavorites(article.id))
//    check(ArticlesStatement.findArticlesFavoriteCount(NonEmptyList.of(article.id)))
    check(ArticlesStatement.findFavoritedsByUserId(NonEmptyList.of(article.id), article.authorId))
    check(ArticlesStatement.listArticlesByFavoritedUsers(article.authorId, defaultPagination))

  }

}
