package com.real.world.http4s.model

import com.real.world.http4s.model.article.Article.ArticleId
import com.real.world.http4s.model.user.User.UserId

final case class FavoritedRecord(id: Int, userId: UserId, articleId: ArticleId)
