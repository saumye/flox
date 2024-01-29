package ai.flox.home.data

import ai.flox.home.model.HomeAction
import ai.flox.network.NetworkResource
import ai.flox.network.newsapi.NewsService
import ai.flox.state.Resource
import ai.flox.storage.news.NewsDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val newsService: NewsService,
    private val newsDAO: NewsDAO
) {

    fun refreshNews(): Flow<HomeAction> {
        return flow {
            val headlines = newsService.headlines()
            if (headlines is NetworkResource.Success) {
                headlines.data?.let { topHeadlinesResponse ->
                    for (headline in topHeadlinesResponse.articles) {
                        newsDAO.insertOrUpdate(headline.toDomain().toLocal())
                    }
                    emit(HomeAction.CreateOrUpdateArticles(Resource.Success(topHeadlinesResponse.articles.map { it.toDomain() })))
                }
            }
        }
    }

    fun getNews(): Flow<HomeAction> {
        return flow {
            emit(HomeAction.LoadArticles(Resource.Success(newsDAO.getAll().map { it.toDomain() })))
        }.flowOn(Dispatchers.IO).catch {
            emit(HomeAction.LoadArticles(Resource.Failure(Exception(it))))
        }
    }
}