package ai.flox.network

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Generates Retrofit service proxy
 */
object ServiceGenerator {

    private fun <S> createService(
        serviceClass: Class<S>,
        baseUrl: String,
        httpClient: OkHttpClient
    ): S {
        val serviceBuilder = Retrofit.Builder()
            .client(httpClient)
            .callFactory { request: Request ->
                httpClient.newCall(
                    request.newBuilder()
                        .build()
                )
            }.addConverterFactory(MoshiConverterFactory.create())
        return serviceBuilder.baseUrl(baseUrl).build().create(serviceClass)
    }

}
