package com.real.world.http4s.model.article

import com.real.world.http4s.model.UserId
import com.real.world.http4s.model.ArticleId

final case class FavoritedRecord(id: Int, userId: UserId, articleId: ArticleId)
