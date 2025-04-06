package ai.flox.network.openai

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp Interceptor that adds an authorization token header
 */
class AuthenticationInterceptor(private val token: String) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
            .newBuilder()
            .header("Authorization", "Bearer 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")
            .build()
        return chain.proceed(request)
    }
}