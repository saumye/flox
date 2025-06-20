package ai.flox.home.data

import ai.flox.home.model.HomeAction
import ai.flox.home.model.NewsCategory
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
    
    fun refreshNewsByCategory(category: NewsCategory = NewsCategory.TOP): Flow<HomeAction> {
        return flow {
            val headlines = newsService.headlines(category=category.apiValue)
            if (headlines is NetworkResource.Success) {
                headlines.data?.let { topHeadlinesResponse ->
                    for (headline in topHeadlinesResponse.articles) {
                        newsDAO.insertOrUpdate(headline.toDomain(category.apiValue).toLocal())
                    }
                    emit(HomeAction.LoadCategoryArticles(
                        category,
                        Resource.Success(topHeadlinesResponse.articles.map { it.toDomain(category.apiValue) })
                    ))
                }
            }
        }.flowOn(Dispatchers.IO).catch {
            emit(HomeAction.LoadCategoryArticles(category, Resource.Failure(Exception(it))))
        }
    }
    
    fun getNewsByCategory(category: NewsCategory = NewsCategory.TOP): Flow<HomeAction> {
        return flow {
            emit(HomeAction.LoadCategoryArticles(
                category,
                Resource.Success(newsDAO.getByCategory(category.apiValue).map { it.toDomain() })
            ))
        }.flowOn(Dispatchers.IO).catch {
            emit(HomeAction.LoadCategoryArticles(category, Resource.Failure(Exception(it))))
        }
    }
}