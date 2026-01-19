package sdk.chat.demo.robot.api

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.BuildConfig
import sdk.chat.demo.robot.extensions.DeviceIdHelper
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.util.TimeZone

class CommonHeadersInterceptor() : Interceptor {
    private val deviceId: String
    private val userAgent: String
    private val versionCode: String
    private val timeZone: String = TimeZone.getDefault().id

    init {
        // 在构造时初始化
        deviceId = DeviceIdHelper.getDeviceId(MainApp.getContext())
        userAgent = "Android/${Build.VERSION.RELEASE} " +
                "App/${BuildConfig.VERSION_NAME} " +
                "Package/${BuildConfig.APPLICATION_ID}"
        versionCode = BuildConfig.VERSION_CODE.toString()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-App-Version", versionCode)
            .addHeader("X-App-VersionName", BuildConfig.VERSION_NAME)
            .addHeader("platform", "android")
            .addHeader("X-Bundle-ID", BuildConfig.APPLICATION_ID)
            .addHeader("bundleId", BuildConfig.APPLICATION_ID)
            .addHeader("X-Device-ID", deviceId)
            .addHeader("X-Timezone", timeZone)
            .addHeader("X-Language", LanguageUtils.getAppLanguage(MainApp.getContext(), false))
            .build()

        return chain.proceed(request)
    }

    private fun getUserAgent(): String {
        return "Android/${Build.VERSION.RELEASE} " +
                "App/${BuildConfig.VERSION_NAME} " +
                "Package/${BuildConfig.APPLICATION_ID}"
    }
}