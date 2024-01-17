package ai.flox.network

import ai.flox.network.openai.AuthenticationInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Singleton
    @Provides
    fun provideGSON(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().addInterceptor(AuthenticationInterceptor(BuildConfig.OPENAIAPI_KEY)).build()
    }
}