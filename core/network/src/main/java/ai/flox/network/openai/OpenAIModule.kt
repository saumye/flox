package ai.flox.network.openai

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenAIModule {

    @Singleton
    @Provides
    internal fun provideOpenAIAPI(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): OpenAIAPI {
        val retrofit =
            Retrofit.Builder()
                .baseUrl(OpenAIAPI.ENDPOINT)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        return retrofit.create(OpenAIAPI::class.java)
    }
}