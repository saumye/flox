package ai.flox.storage.conversation

import ai.flox.storage.BaseDAO
import ai.flox.storage.conversation.model.ConversationEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface ConversationDAO : BaseDAO {
    @Query("SELECT * FROM conversation")
    suspend fun getAll(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(vararg conversation: ConversationEntity)
}