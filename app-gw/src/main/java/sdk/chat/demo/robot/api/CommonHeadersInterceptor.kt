package sdk.chat.demo.robot.api

import okhttp3.*
import android.content.Context
import android.os.Build
import sdk.chat.demo.pre.BuildConfig

class CommonHeadersInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("versionCode", BuildConfig.VERSION_CODE.toString())
            .addHeader("platform", "android")
            .addHeader("packageName", BuildConfig.APPLICATION_ID)
            .addHeader("debug", BuildConfig.DEBUG.toString())
            .addHeader("userAgent", getUserAgent())
            .build()

        return chain.proceed(request)
    }

    private fun getUserAgent(): String {
        return "Android/${Build.VERSION.RELEASE} " +
                "App/${BuildConfig.VERSION_NAME} " +
                "Package/${BuildConfig.APPLICATION_ID}"
    }
}