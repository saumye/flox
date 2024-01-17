package ai.flox.storage

import ai.flox.storage.chat.ChatDAO
import ai.flox.storage.conversation.ConversationDAO
import ai.flox.storage.news.NewsDAO
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Singleton
    @Provides
    fun provideDB(@ApplicationContext context: Context) : AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java, "flox-sample-db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideChatDAO(database: AppDatabase): ChatDAO {
        return database.chatDao()
    }

    @Singleton
    @Provides
    fun provideConversationDAO(database: AppDatabase): ConversationDAO {
        return database.conversationDAO()
    }

    @Singleton
    @Provides
    fun provideNewsDAO(database: AppDatabase): NewsDAO {
        return database.newsDAO()
    }
}