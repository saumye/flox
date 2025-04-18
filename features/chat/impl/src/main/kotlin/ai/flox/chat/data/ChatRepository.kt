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
    private val chatDAO: ChatDAO,
    @ApplicationContext private val application: Context
) {
    private val TAG: String = "ChatRepository"
    private val s2t: S2T = S2T(application)
    private val tts: TTS = TTS(application)

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

    fun recordMessage(message: ChatMessage): Flow<ChatAction> {
        return callbackFlow {
            s2t.startStreaming(object : Whisper.WhisperListener {
                override fun onUpdateReceived(message: String) {
                    Log.d(TAG, "Update is received, Message: $message")
                }

                override fun onResultReceived(result: String) {
                    Log.d(TAG, "Result: $result")
                    trySendBlocking(
                        ChatAction.UpdateVoiceInput(
                            message = result,
                            conversation = message.conversation
                        )
                    )
                }
            })
            awaitClose {
                s2t.startStreaming(null)
            }
        }
    }
//            callbackFlow {
//                s2t.setListener(object : Whisper.WhisperListener {
//                    override fun onUpdateReceived(message: String) {
//                        Log.d(TAG, "Update is received, Message: $message")
//                    }
//
//                    override fun onResultReceived(result: String) {
//                        Log.d(TAG, "Result: $result")
//                        trySendBlocking(
//                            ChatAction.SendMessage(
//                                message = result,
//                                conversation = message.conversation
//                            )
//                        )
//                    }
//                })
//                s2t.startStreaming()
//            }
//        } else {
//            flow {
//                s2t.startStreaming()
//            }
//        }
//    }

    fun sendMessage(message: ChatMessage): Flow<ChatAction> {
        return flow {
            chatDAO.insertOrUpdate(message.toLocal(message.conversation))
            emit(
                ChatAction.CreateOrUpdateMessages(Resource.Success(message))
            )
            val response = openAIService.completions(OpenAIRequest.fromDomain(message.message))
            if (response is NetworkResource.Success) {
                response.data?.let {
                    val textStr = it.toDomain(
                        Date(System.currentTimeMillis()),
                        message.conversation
                    ).message
                    for(text in textStr.split(".",",","!")) if(text.isNotEmpty()) tts.generate(text)
                    chatDAO.insertOrUpdate(
                        it.toDomain(
                            Date(System.currentTimeMillis()),
                            message.conversation
                        ).toLocal(message.conversation)
                    )
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
                                it.toDomain(
                                    Date(System.currentTimeMillis()),
                                    message.conversation
                                )
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