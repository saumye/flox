package ai.flox.conversation.data

import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationAction
import ai.flox.state.Resource
import ai.flox.storage.conversation.ConversationDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ConversationRepository @Inject constructor(
    private val conversationDAO: ConversationDAO
) {
    fun getConversations(): Flow<ConversationAction> {
        return flow {
            emit(
                ConversationAction.LoadConversations(
                    Resource.Success(conversationDAO.getAll().map { it.toDomain() })
                )
            )
        }.flowOn(Dispatchers.IO).catch {
            emit(ConversationAction.LoadConversations(Resource.Failure(Exception(it))))
        }
    }

    fun getConversation(): Flow<ConversationAction> {
        return flow {
            emit(
                ConversationAction.LoadConversation(
                    Resource.Success(conversationDAO.get("").toDomain() )
                )
            )
        }.flowOn(Dispatchers.IO).catch {
            emit(ConversationAction.LoadConversation(Resource.Failure(Exception(it))))
        }
    }

    fun addConversation(conversation: Conversation): Flow<ConversationAction> {
        return flow {
            conversationDAO.insertOrUpdate(conversation.toLocal())
            emit(
                ConversationAction.CreateOrUpdateConversation(
                    Resource.Success(
                        (conversation)
                    )
                )
            )
        }.flowOn(Dispatchers.IO).catch {
            emit(
                ConversationAction.CreateOrUpdateConversation(
                    Resource.Failure(
                        Exception(it),
                        conversation.copy()
                    )
                )
            )
        }
    }
}