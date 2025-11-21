package sdk.chat.demo.robot.audio

import android.os.Bundle
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONException
import org.json.JSONObject
import org.tinylog.Logger
import sdk.chat.core.dao.Message
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.utils.AppBackgroundMonitor.StopListener
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.util.Locale

object TTSHelper {
    private val TAG = "TTSHelper"
    private val ttsType = 1
    private var textToSpeech: TextToSpeech? = null
    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>
    private var mCurTtsText = ""
    private var playingMsg: Message? = null

    // Options Default Value
    private const val mTtsSilenceDuration = 0
    private const val mTtsSpeakSpeed = 10
    private const val mTtsAudioVolume = 10
    private const val mTtsAudioPitch = 10

    // Novel Scenario Related
    private var mTtsSynthesisFromPlayer = false
    private const val mTtsPlayingProgress = 0.0
    private var mTtsPlayingIndex = -1
    private var mTtsSynthesisIndex = 0
    private var mTtsSynthesisText: MutableList<String> = mutableListOf()
    private var mTtsSynthesisMap: MutableMap<String?, Int?>? = mutableMapOf()

    // Engine State
    private var mEngineInited = false
    private var mConnectionCreated = false
    private var mEngineStarted = false
    private var mPlayerPaused = false
    private var mAudioManager: AudioManager? = null
    private var mResumeOnFocusGain = true
    private var mPlaybackNowAuthorized = false
    private var audioFocusRequest: AudioFocusRequest? = null
    var voiceType = "BV001_streaming"
//    var customeVoiceType:String? = null
//    var speaker = "volc.megatts.default"


    fun initTTS(context: AppCompatActivity) {
        // 检查 TTS 是否可用
        // 注册 ActivityResultLauncher

        voiceType =
            context.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("db_voice_type", "BV026_streaming")
                .toString()

        //FIXME 初始化系统tts
//        ttsCheckLauncher =
//            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
//                    initSystemTTS(context) // TTS 可用，初始化
//                } else {
//                    // 提示用户安装 TTS 数据
//                    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
//                    context.startActivity(installIntent)
//                }
//            }
//
//        // 检查 TTS 数据
//        val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
////        ttsCheckLauncher.launch(checkIntent)
//
//
//        if (checkIntent.resolveActivity(context.packageManager) != null) {
//            // 确认有 TTS 引擎后再启动
//            ttsCheckLauncher.launch(checkIntent)
//        } else {
//            // 设备完全无 TTS 支持时的处理
////            handleNoTtsEngine()
//            Toast.makeText(context, "暂不支持语音播放", Toast.LENGTH_SHORT).show()
//        }

        initDoubaoTTS(context)
    }

    private fun initSystemTTS(context: AppCompatActivity) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show()
                } else {
//                    speek("你好, Android TTS")
//                    textToSpeech.speak("Hello, Android TTS", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Toast.makeText(context, "onStart", Toast.LENGTH_SHORT).show()
            }

            override fun onDone(utteranceId: String?) {

                //通知老的播放按键恢复一下
                setPlayingMsg(null)
                Toast.makeText(context, "onDone", Toast.LENGTH_SHORT).show()
            }

            override fun onError(utteranceId: String?) {
                setPlayingMsg(null)
                Toast.makeText(context, "playError", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun speek(text: String, msgId: String) {
    }

    fun stop() {
    }

    fun clear() {
    }

    fun resetVoiceType() {
    }

    //doubao

    private fun initDoubaoTTS(context: AppCompatActivity) {
        mAudioManager =
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        ChatSDK.appBackgroundMonitor().addListener(mBackgroundListener);
    }

    private var mBackgroundListener: StopListener =
        object : StopListener {
            override fun didStop() {
                pausePlayback()
            }

        }

    private var mAFChangeListener: OnAudioFocusChangeListener =
        object : OnAudioFocusChangeListener {
            override fun onAudioFocusChange(focusChange: Int) {
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(
                            TAG,
                            "onAudioFocusChange: AUDIOFOCUS_GAIN, $mResumeOnFocusGain"
                        )
                        if (mResumeOnFocusGain) {
                            mResumeOnFocusGain = false
                            resumePlayback()
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS")
                        mResumeOnFocusGain = false
                        pausePlayback()
                        mPlaybackNowAuthorized = false
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT")
                        mResumeOnFocusGain = mEngineStarted
                        pausePlayback()
                    }
                }
            }

        }

    private fun pausePlayback() {
    }

    private fun resumePlayback() {
    }

    private fun controlPlayingStatus() {
        Log.d(
            TAG,
            "Pause or resume player, current player status: $mPlayerPaused"
        )
        if (mPlayerPaused) {
            if (!mPlaybackNowAuthorized) { // AudioFocus 被其他 APP 占用，需要再次获取
                AcquireAudioFocus()
            }
            resumePlayback()
        } else {
            pausePlayback()
        }
        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(this.playingMsg))
    }




    private fun resetTtsContext() {
        mTtsPlayingIndex = -1
        mTtsSynthesisIndex = 0
        mTtsSynthesisFromPlayer = false
        mTtsSynthesisText.clear()
        mTtsSynthesisMap?.clear()
    }

    private fun prepareTextList(): Boolean {
        resetTtsContext()

//        var ttsText = playingMsg.getf
//        if (ttsText.isEmpty()) {
//            ttsText =
//                "愿中国青年都摆脱冷气，只是向上走，不必听自暴自弃者流的话。能做事的做事，能发声的发声。有一分热，发一分光。就令萤火一般，也可以在黑暗里发一点光，不必等候炬火。此后如竟没有炬火：我便是唯一的光。"
//        }

        //【必需配置】需合成的文本，不可超过 80 字
        if (mTtsSynthesisText == null || mTtsSynthesisText.isEmpty()) {
            // 使用下面几个标点符号来分句，会让通过 MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS 返回的播放进度更加准确
            val tmp: Array<String?> =
                mCurTtsText.split("[;|!|?|。|！|？|；|…|,|.]".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (j in tmp.indices) {
                AddSentence(tmp[j].toString())
            }
        }
        Log.d(TAG, "Synthesis text item num: " + mTtsSynthesisText!!.size)
        return !mTtsSynthesisText.isEmpty()
    }

    private fun AddSentence(text: String) {
        val tmp = text.trim { it <= ' ' }
        if (!tmp.isEmpty()) {
            mTtsSynthesisText.add(tmp)
        }
    }

    private fun setVoiceTypeByText(text: String) {
        var configs = ImageApi.getGwConfigs()
        var defaultVoiceTypes = configs.defaultVoiceTypes
        if (defaultVoiceTypes != null) {
            var lang = LanguageUtils.getAppLanguage(MainApp.getContext(), false)
            var isEnLangText = LanguageUtils.getTextLanguage(text) == Locale.US
            if (lang?.isNotEmpty() == true) {
                var dvt: String? = null
                if (isEnLangText) {
                    dvt = defaultVoiceTypes["en"]
                } else if (lang.contains("Hant", ignoreCase = true)) {
                    dvt = defaultVoiceTypes["zh-hant"]
                } else if (lang.contains("zh", false)
                    && (lang.contains("tw",false) || lang.contains("hk", false))) {
                    dvt = defaultVoiceTypes["zh-hant"]
                } else {
                    dvt = defaultVoiceTypes["zh-hans"]
                }
                if (dvt != null) {
                    voiceType = dvt
                    Logger.error { "set default voicetype:${voiceType},isEnLangText:$isEnLangText" }
                    Log.d(TAG, "set default voicetype:${voiceType},isEnLangText:$isEnLangText")
                }
            }
        }
    }


    private fun configStartTtsParams() {
    }

    private fun configSynthesisParams() {


    }

    private fun AcquireAudioFocus() {
//AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE or AUDIOFOCUS_GAIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(mAFChangeListener)
                    .build()

            audioFocusRequest = focusRequest
            val result = mAudioManager?.requestAudioFocus(focusRequest)

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlaybackNowAuthorized = true
            } else {
                mPlaybackNowAuthorized = false
            }
        } else {
            // 兼容旧版本
            val result = mAudioManager?.requestAudioFocus(
                mAFChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlaybackNowAuthorized = true
            } else {
                mPlaybackNowAuthorized = false
            }
        }
    }


    private fun configInitParams() {
    }


    fun getPlayingMsg(): Message? {
        return playingMsg
    }

    fun isPlayerPaused(): Boolean {
        return mPlayerPaused
    }

    fun setPlayingMsg(newPlaying: Message?): Boolean {
        if (this.playingMsg == null) {
            if (newPlaying != null) {
                this.playingMsg = newPlaying
            } else {
                return false
            }
        } else {
            val oldPlaying: Message? = this.playingMsg
            if (newPlaying != null && oldPlaying?.id == newPlaying.id) {
                return true
            } else {
                this.playingMsg = newPlaying
            }
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(oldPlaying))
        }
        if (newPlaying != null) {
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(newPlaying))
        }
        return true
    }
}