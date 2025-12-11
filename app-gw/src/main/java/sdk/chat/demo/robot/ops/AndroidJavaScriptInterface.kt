package sdk.chat.demo.robot.ops

import android.content.Intent
import org.json.JSONObject
import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import android.os.Build
import android.net.Uri
import android.widget.Toast
import sdk.chat.demo.robot.activities.WebViewActivity

class AndroidJavaScriptInterface(private val context: Context) {

    // 显示Toast
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // 获取设备信息
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return JSONObject().apply {
            put("platform", "Android")
            put("version", Build.VERSION.RELEASE)
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
        }.toString()
    }

    // 调用系统功能 - 拨打电话
    @JavascriptInterface
    fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    }

    // 获取位置信息（需要权限）
    @JavascriptInterface
    fun getCurrentLocation(): String {
        // 实现定位逻辑
        return JSONObject().apply {
            put("latitude", 39.9042)
            put("longitude", 116.4074)
            put("city", "Beijing")
        }.toString()
    }

    // 保存数据到SharedPreferences
    @JavascriptInterface
    fun saveData(key: String, value: String) {
        val prefs = context.getSharedPreferences("WebViewPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    // 从SharedPreferences读取数据
    @JavascriptInterface
    fun getData(key: String): String {
        val prefs = context.getSharedPreferences("WebViewPrefs", Context.MODE_PRIVATE)
        return prefs.getString(key, "") ?: ""
    }

    // 带回调的方法
    @JavascriptInterface
    fun processData(input: String, callbackId: String) {
        // 处理数据
        val result = "Processed: $input"

        // 通过WebView返回结果
        (context as? Activity)?.runOnUiThread {
            val webView = (context as WebViewActivity).getWebView()
            webView.evaluateJavascript(
                "javascript:window.onNativeResult('$callbackId', '$result')",
                null
            )
        }
    }
}