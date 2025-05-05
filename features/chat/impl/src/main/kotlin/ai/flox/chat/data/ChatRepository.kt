package ai.flox.chat.data

import ai.flox.asr.Whisper
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.SyncStatus
import ai.flox.conversation.model.Conversation
import ai.flox.network.NetworkResource
import ai.flox.network.openai.OpenAIService
import ai.flox.network.openai.models.OpenAIRequest
import ai.flox.state.Resource
import ai.flox.storage.chat.ChatDAO
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Date
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val openAIService: OpenAIService,
    private val localLlm: LocalLlm,
    private val chatDAO: ChatDAO
) {
    private val TAG: String = "ChatRepository"

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

    fun sendMessage(message: ChatMessage, isOnlineMode: Boolean = true): Flow<ChatAction> {
        return flow {
            // Save the user message to database
            chatDAO.insertOrUpdate(message.toLocal(message.conversation))
            
            // Emit the user message to UI
            emit(
                ChatAction.CreateOrUpdateMessages(Resource.Success(message))
            )
            
            // Get response from appropriate service based on mode
            val response = if (isOnlineMode) {
                // Use online service
                openAIService.completions(OpenAIRequest.fromDomain(message.message))
            } else {
                // Use offline service
                localLlm.completions(message)
            }
            
            // Process the response
            if (response is NetworkResource.Success) {
                response.data?.let {
                    // Create AI message
                    val aiMessage = it.toDomain(
                        Date(System.currentTimeMillis()),
                        message.conversation
                    )
                    
                    // Save AI message to database
                    chatDAO.insertOrUpdate(aiMessage.toLocal(message.conversation))
                    
                    // Update user message status
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                message.copy(messageState = SyncStatus.COMPLETED)
                            )
                        )
                    )
                    
                    // Emit AI response
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                aiMessage.copy(messageState = SyncStatus.COMPLETED)
                            )
                        )
                    )
                }
            } else if (response is NetworkResource.Failure) {
                // Handle failure
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