package ai.flox.chat.data

import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.SyncStatus
import ai.flox.conversation.model.Conversation
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
import java.util.Date
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val openAIService: OpenAIService,
    private val chatDAO: ChatDAO
) {

    fun getChatMessages(conversation: Conversation): Flow<ChatAction> {
        return flow {
            emit(
                ChatAction.LoadMessages(
                    Resource.Success(chatDAO.getAll().map { it.toDomain(conversation) })
                )
            )
        }.flowOn(Dispatchers.IO).catch {
            emit(ChatAction.LoadMessages(Resource.Failure(Exception(it))))
        }
    }

    fun sendMessage(message: ChatMessage): Flow<ChatAction> {
        return flow {
            chatDAO.insertOrUpdate(message.toLocal(message.conversation))
            emit(
                ChatAction.CreateOrUpdateMessages(Resource.Success(message))
            )
            val response = openAIService.completions(OpenAIRequest.fromDomain(message.message))
            if (response is NetworkResource.Success) {
                response.data?.let {
                    chatDAO.insertOrUpdate(it.toDomain(Date(System.currentTimeMillis()), message.conversation).toLocal(message.conversation))
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                message.copy(
                                    messageState = SyncStatus.COMPLETED
                                )
                            )
                        )
                    )
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                it.toDomain(Date(System.currentTimeMillis()), message.conversation)
                                    .copy(messageState = SyncStatus.COMPLETED)
                            )
                        )
                    )
                }
            } else if (response is NetworkResource.Failure) {
                emit(
                    ChatAction.CreateOrUpdateMessages(
                        Resource.Failure(
                            error = response.error,
                            data = message.copy(messageState = SyncStatus.FAILED_PERMANENTLY)
                        )
                    )
                )
            }
        }.flowOn(Dispatchers.IO).catch {
            emit(
                ChatAction.CreateOrUpdateMessages(
                    Resource.Failure(
                        Exception(it),
                        message.copy(messageState = SyncStatus.FAILED_PERMANENTLY)
                    )
                )
            )
        }
    }
}