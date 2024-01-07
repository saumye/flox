package ai.flox.chat.data

import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.SyncStatus
import ai.flox.network.NetworkResource
import ai.flox.network.openai.OpenAIService
import ai.flox.network.openai.models.OpenAIRequest
import ai.flox.state.Resource
import ai.flox.storage.chat.ChatDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val openAIService: OpenAIService,
    private val chatDAO: ChatDAO
) {

    fun getChatMessages(): Flow<ChatAction> {
        return flow {
            emit(
                ChatAction.LoadMessages(
                    Resource.Success(
                        chatDAO.getAll().map { it.toDomain() })
                )
            )
        }.flowOn(Dispatchers.IO).catch {
            emit(ChatAction.LoadMessages(Resource.Failure(Exception(it))))
        }
    }

    fun sendMessage(message: ChatMessage): Flow<ChatAction> {
        return flow {
            chatDAO.insertAll(message.toLocal())
            emit(
                ChatAction.CreateOrUpdateMessages(
                    Resource.Success(
                        listOf(message)
                    )
                )
            )
            val response = openAIService.completions(OpenAIRequest.fromDomain(message.message))
            if (response is NetworkResource.Success) {
                response.data?.let {
                    chatDAO.insertAll(it.toDomain().toLocal())
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                listOf(message.copy(messageState = SyncStatus.COMPLETED))
                            )
                        )
                    )
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                listOf(it.toDomain().copy(messageState = SyncStatus.COMPLETED))
                            )
                        )
                    )
                }
            } else if (response is NetworkResource.Failure) {
                emit(
                    ChatAction.CreateOrUpdateMessages(
                        Resource.Failure(
                            error = response.error,
                            data = listOf(message.copy(messageState = SyncStatus.FAILED_PERMANENTLY))
                        )
                    )
                )
            }
        }.flowOn(Dispatchers.IO).catch {
            emit(
                ChatAction.CreateOrUpdateMessages(
                    Resource.Failure(
                        Exception(it),
                        listOf(message.copy(messageState = SyncStatus.FAILED_PERMANENTLY))
                    )
                )
            )
        }
    }
}