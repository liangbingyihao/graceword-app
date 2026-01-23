package sdk.chat.demo.robot.activities

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleObserver
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.utils.PermissionRequestHandler
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.EditCardActivity
import sdk.chat.demo.robot.activities.SettingWallpaperActivity
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.ImageSaveUtils
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.handlers.CardGenerator
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.LimitCounter
import sdk.chat.demo.robot.handlers.OffscreenScreenshotHelper
import sdk.chat.demo.robot.handlers.OffscreenScreenshotHelper.ButtonConfig
import sdk.chat.demo.robot.handlers.SpeechToTextHelper
import sdk.chat.demo.robot.push.UpdateTokenWorker
import sdk.chat.demo.robot.utils.ToastHelper
import sdk.chat.demo.robot.utils.WallpaperUtils
import sdk.guru.common.DisposableMap
import java.util.Locale

data class SpinnerItem(val label: String, val value: String) {
    override fun toString(): String = label
}

class SpeechToTextActivity : AppCompatActivity(), View.OnClickListener,
    LifecycleObserver {
    private lateinit var speechToTextHelper: SpeechToTextHelper
    private lateinit var resultTextView: TextView
    private lateinit var ttsParams: TextView
    private lateinit var ttsError: TextView
    private lateinit var tvTaskIndex: EditText
    private lateinit var ttsVoiceTypeSpinner: Spinner
    protected var dm: DisposableMap = DisposableMap()

//    // Engine
//    private var mSpeechEngine: SpeechEngine? = null
//    private var mEngineStarted = false
//
//    // StreamRecorder
//    private var mStreamRecorder: SpeechStreamRecorder? = null

    // Statistics
    private var mFinishTalkingTimestamp: Long = -1
//    private lateinit var startButton: Button
//    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_to_text)

        resultTextView = findViewById(R.id.resultTextView)
        tvTaskIndex = findViewById(R.id.taskIndex)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.updateToken).setOnClickListener(this)
        findViewById<View>(R.id.startButton).setOnClickListener(this)
        findViewById<View>(R.id.stopButton).setOnClickListener(this)
        findViewById<View>(R.id.clearCache).setOnClickListener(this)
        findViewById<View>(R.id.setTask).setOnClickListener(this)
        findViewById<View>(R.id.setPrompt).setOnClickListener(this)
        findViewById<View>(R.id.getLog).setOnClickListener(this)
        findViewById<View>(R.id.billing).setOnClickListener(this)
//        findViewById<View>(R.id.startdbasr).setOnClickListener(this)
//        findViewById<View>(R.id.initdbasr).setOnClickListener(this)
//        findViewById<View>(R.id.cleardbasr).setOnClickListener(this)

        ttsParams = findViewById<TextView>(R.id.ttsparams)
        ttsError = findViewById<TextView>(R.id.ttserror)


//        var mRecordBtn = findViewById<View>(R.id.startdbasr)
//
//        mRecordBtn.setOnTouchListener { v: View?, event: MotionEvent? ->
//            if (event!!.getAction() == MotionEvent.ACTION_DOWN) {
//                Log.i(tagAsr, "Record: Action down")
//                recordBtnTouchDown()
//                return@setOnTouchListener true
//            } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                Log.i(tagAsr, "Record: Action up")
//                recordBtnTouchUp()
//                return@setOnTouchListener true
//            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
//                Log.i(tagAsr, "Record: Action cancel")
//                recordBtnTouchUp()
//                return@setOnTouchListener true
//            }
//            false
//        }

//        // 检查并请求录音权限
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.RECORD_AUDIO),
//                REQUEST_RECORD_AUDIO_PERMISSION
//            )
//        }
        // 初始化语音识别
        speechToTextHelper = SpeechToTextHelper(this) { text, isFinal ->
            runOnUiThread {
                if (isFinal) {
                    resultTextView.text = "识别结果: $text"
                    Toast.makeText(this, "识别完成", Toast.LENGTH_SHORT).show()
                } else {
                    resultTextView.text = "识别中: $text"
                }
            }
        }

        setVoiceTypeSpinner()


        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.Error))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
                    val params = networkEvent!!.getData()
                    val errType = params.getOrDefault("type", "") as String?
                    val msg = params.getOrDefault("msg", "tts error...") as String?
                    if ("tts" == errType) {
                        ToastHelper.show(this, msg)
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("恩语", msg)
                        clipboard.setPrimaryClip(clip)
                        ttsError.setText(msg)
                    } else if ("tts.params" == errType) {
                        ttsParams.setText(msg)
                    }
                })
        )

        Log.d(
            "LanguageTag",
            "lang:" + Locale.getDefault().toLanguageTag() + "," + getString(R.string.questions)
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        val wic: WindowInsetsControllerCompat? =
//            ViewCompat.getWindowInsetsController(getWindow().getDecorView())
//        if (wic != null) {
//            // true表示Light Mode，状态栏字体呈黑色，反之呈白色
//            wic.setAppearanceLightStatusBars(true)
//        }
        val windowInsetsController: WindowInsetsControllerCompat? =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false) // 白色文字
        }
    }

    fun setVoiceTypeSpinner() {
        ttsVoiceTypeSpinner = findViewById(R.id.ttsVoiceTypeSpinner)
        var voiceTypes = ImageApi.getGwConfigs().dbVoiceTypes
        var languages = listOf<SpinnerItem>()
        if (voiceTypes != null && !voiceTypes.isEmpty()) {
            languages = voiceTypes.map { item ->
                SpinnerItem(
                    item.name.toString(),
                    item.voiceType.toString()
                )
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ttsVoiceTypeSpinner.adapter = adapter
//        val languages = listOf(
//            SpinnerItem("通用-灿灿", "BV700_streaming"),
//            SpinnerItem("通用-女声", "BV001_streaming"),
//            SpinnerItem("通用-男声", "BV002_streaming"),
//            SpinnerItem("有声阅读-擎苍", "BV701_streaming"),
//            SpinnerItem("有声阅读-通用赘婿", "BV119_streaming"),
//            SpinnerItem("有声阅读-儒雅青年", "BV102_streaming"),
//            SpinnerItem("有声阅读-甜宠少御", "BV113_streaming"),
//            SpinnerItem("有声阅读-甜宠少御", "BV115_streaming"),
//        )


        var voiceType =
            getSharedPreferences("app_prefs", MODE_PRIVATE).getString(
                "db_voice_type",
                "BV026_streaming"
            )

        for (i in 0..<adapter.count) {
            if (adapter.getItem(i)?.value == voiceType) {
                ttsVoiceTypeSpinner.setSelection(i)
                break
            }
        }

        ttsVoiceTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = languages[position]
                // 使用selected.value获取实际值
//                Toast.makeText(
//                    this@SpeechToTextActivity,
//                    "Selected value: ${selected.value}",
//                    Toast.LENGTH_SHORT
//                ).show()
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit() {
                        putString("db_voice_type", selected.value)
                    }
                if (TTSHelper.voiceType != selected.value) {
                    TTSHelper.resetVoiceType()
                }
                TTSHelper.voiceType = selected.value
//                TTSHelper.speaker = selected.label
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            REQUEST_RECORD_AUDIO_PERMISSION -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this, "需要录音权限才能使用语音识别", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        speechToTextHelper.destroy()
        dm.dispose();
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.home -> {
                finish()
            }

            R.id.updateToken -> {
                UpdateTokenWorker.forceUpdateToken(this@SpeechToTextActivity)
            }

            R.id.startButton -> {
//                speechToTextHelper.startListening()
                LanguageUtils.switchLanguage(this, "zh-Hans")
                recreate()
            }

            R.id.stopButton -> {
//                speechToTextHelper.stopListening()
                LanguageUtils.switchLanguage(this, "en-US")
                recreate()
            }

            R.id.billing -> {
//                speechToTextHelper.stopListening()
//                BillingActivity.start(this@SpeechToTextActivity,"test")
                captureScreenshotWithFooter()
//                CampaignInfoActivity.start(this@SpeechToTextActivity, "mini")
//                EditCardActivity.start(this@SpeechToTextActivity,directUrl="https://api-test.kolacdn.xyz/public/spring.html")
            }

            R.id.setPrompt -> {
                startActivity(
                    Intent(
                        this@SpeechToTextActivity,
                        LoginActivity::class.java
                    )
                )
//                WallpaperGuideUtil(this@SpeechToTextActivity).guideToSetLiveWallpaper()
            }

            R.id.clearCache -> {

                // 保存已经显示过引导页的状态
//                getSharedPreferences("app_prefs", MODE_PRIVATE)
//                    .edit() {
//                        putBoolean("has_shown_guide", false)
//                    }
                deleteSharedPreferences("app_prefs")

//                val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
//                if(threadHandler.welcome!=null){
//                    ChatSDK.db().delete(threadHandler.welcome)
//                }
                JsonCacheManager.save(this@SpeechToTextActivity, "gwTaskProcess", "")
                JsonCacheManager.save(this@SpeechToTextActivity, "gwDaily", "")
                LimitCounter.clearAll(this@SpeechToTextActivity)
            }

            R.id.setTask -> {
                var taskIndex: Int = tvTaskIndex.text.toString().toInt()
                DailyTaskHandler.testTaskDetail(taskIndex)
            }

            R.id.getLog -> {
//                LogUploader.uploadLogs(this@SpeechToTextActivity)
//                val clipboard =getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
//                val clip = ClipData.newPlainText("恩语", LogHelper.logStr)
//                clipboard.setPrimaryClip(clip)
            }

//            R.id.initdbasr -> {
//                if (mEngineStarted) {
//                    return
//                }
//                initEngine()
//            }
//
//            R.id.cleardbasr -> {
//                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_STOP_ENGINE, "")
//            }
        }
    }

    private fun captureScreenshotWithFooter() {
        val disposable = PermissionRequestHandler
            .requestWriteExternalStorage(this@SpeechToTextActivity)
            .andThen<Bitmap?>( // After permission is granted, execute the following operations
                Observable.create<Bitmap?>(ObservableOnSubscribe { emitter: ObservableEmitter<Bitmap?>? ->
                    OffscreenScreenshotHelper.screenshot(
                        this@SpeechToTextActivity,
                        headerUrl = "https://cdn.grace-word.com/app/26newyear/c29e1c5c9b7d47e3a78aec1395cb520d.webp",
//                        textContents = listOf(
//                            "这是内容标题",
//                            "1这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描述...这是详细的内容描述...这是详细的内容描述...述...",
//                            "2这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描述...这是详细的内容描述...这是详细的内容描述...述...",
//                            "3这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描述...这是详细的内容描述...这是详细的内容描述...述...",
//                            "4这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描这是详细的内容描述\n...这是详细的内容描述...\n这是详细的内容描述...这是详细的内容描述...这是详细的内容描述...述...",
//                            "更多内容信息"
//                        ),
                        buttonConfigs = listOf(
                            ButtonConfig("1这是内容标题", R.layout.screenshot_item_user_msg),
                            ButtonConfig("1这是详细的内容描这是详细的内容描述\n\n...这是详细的内容描述", R.layout.screenshot_item_ai_msg),
                            ButtonConfig("这是问题1", R.layout.screenshot_item_user_msg),
                            ButtonConfig("这是问题1", R.layout.screenshot_item_user_msg),
                            ButtonConfig("1这是详细的内容描这是详细的内容描述\n" +
                                    "...这是详细的内容描述", R.layout.screenshot_item_user_msg),
                            ButtonConfig("这是内容标题", R.layout.screenshot_item_user_msg),
                            ButtonConfig("这是问题1", R.layout.screenshot_item_user_msg),
                            ButtonConfig("这是问题1", R.layout.screenshot_item_user_msg),
                            ButtonConfig("1这是详细的内容描这是详细的内容描述\n" +
                                    "...这是详细的内容描述", R.layout.screenshot_item_user_msg),
                            ButtonConfig("2这是内容标题", R.layout.screenshot_item_user_msg),
                            ButtonConfig("1这是内容标题", R.layout.screenshot_item_song),
                        ),
                        onSuccess = { result: Bitmap? ->
                            Toast.makeText(this@SpeechToTextActivity, "截图成功", Toast.LENGTH_SHORT).show()
                            emitter!!.onNext(result!!) // 发送成功结果
                            emitter.onComplete() // 完成
                            Unit
                        }, onFailure = { err: Throwable? ->
                            Toast.makeText(this@SpeechToTextActivity, "截图失败", Toast.LENGTH_SHORT).show()
                            emitter!!.onError(err!!)
                            Unit
                        })
                })
                    .subscribeOn(Schedulers.io())
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                Consumer { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        val bitmapURL = ImageSaveUtils.saveBitmapToGallery(
                            this@SpeechToTextActivity,  // context
                            bitmap,
                            "img_" + System.currentTimeMillis(),
                            Bitmap.CompressFormat.JPEG
                        )
                        if (bitmapURL != null) {
                            ToastHelper.show(
                                this@SpeechToTextActivity,
                                getString(R.string.image_saved)
                            )

                        } else {
                            ToastHelper.show(
                                this@SpeechToTextActivity,
                                getString(R.string.image_save_failed)
                            )
                        }
                    } else {
                        ToastHelper.show(
                            this@SpeechToTextActivity,
                            getString(R.string.image_save_failed)
                        )
                    }
                    bitmap?.recycle()
                }
            )
        dm.add(disposable)


    }
}