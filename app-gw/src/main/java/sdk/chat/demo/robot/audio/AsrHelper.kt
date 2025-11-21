package sdk.chat.demo.robot.audio

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.tinylog.Logger
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object AsrHelper {
    private const val TAG = "AsrHelper"

    // Engine
    private var mEngineStarted = false
    private var isTraditional = false

    private var lastAsrResult: String = ""
    private var lastDefinite: Boolean = false
    private var reportIndex: Int = 0


    fun initAsrEngine() {

    }

    fun startAsr() {

    }


    fun stopAsr() {
    }
}