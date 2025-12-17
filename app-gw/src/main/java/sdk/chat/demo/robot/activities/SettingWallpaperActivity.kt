package sdk.chat.demo.robot.activities

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
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
import sdk.chat.demo.robot.activities.SpeechToTextActivity
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.formatDayAgo
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.CardGenerator
import sdk.chat.demo.robot.handlers.WallpaperConfig
import sdk.chat.demo.robot.utils.WallpaperGuideUtil
import sdk.chat.demo.robot.utils.WallpaperUtils


class SettingWallpaperActivity : BaseActivity(), View.OnClickListener, OnCheckedChangeListener {
    private lateinit var rbDynamic: CheckBox
    private lateinit var rbStatic: CheckBox
    private lateinit var rbHome: CheckBox
    private lateinit var rbLock: CheckBox
    private lateinit var tvOneClick: TextView
    private lateinit var swClickable: Switch
    private var from: String = ""

    //    private var isPending: Boolean = false
    private lateinit var config: WallpaperConfig

    companion object {
        private const val ARG_FROM = "from"
        private const val IMAGE_DATA = "image_data"

        // 提供静态启动方法（推荐）
        fun start(
            context: Context,
            from: String = "",
            imageData: String = "",
        ) {
            val intent = Intent(context, SettingWallpaperActivity::class.java).apply {
                putExtra(ARG_FROM, from)
                putExtra(IMAGE_DATA, imageData)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var request: ImageDaily

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_setting)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.confirm).setOnClickListener(this)
        rbDynamic = findViewById<CheckBox>(R.id.rb_dynamic)
        rbStatic = findViewById<CheckBox>(R.id.rb_static)
        rbHome = findViewById<CheckBox>(R.id.rb_home)
        rbLock = findViewById<CheckBox>(R.id.rb_lock)
        swClickable = findViewById<Switch>(R.id.switch_read)
        tvOneClick = findViewById<TextView>(R.id.one_click_bible)

        rbDynamic.setOnCheckedChangeListener(this)
        rbHome.setOnCheckedChangeListener(this)
        rbLock.setOnCheckedChangeListener(this)
        rbStatic.setOnCheckedChangeListener(this)
        swClickable.setOnCheckedChangeListener(this)


        from = intent.getStringExtra(ARG_FROM).toString()

        if (intent.hasExtra(IMAGE_DATA)) {
            try {
                request = (Gson()).fromJson<ImageDaily?>(
                    intent.getStringExtra(IMAGE_DATA),
                    ImageDaily::class.java
                )
                Log.e("SettingWallpaper", request.greeting)
            } catch (ignored: Exception) {
            }
        }
        initSetting()
    }

    fun initSetting() {
        var configCache = CardApiService.getWallPaperConfig();
        rbDynamic.isChecked = configCache.isDynamic
        swClickable.isChecked = configCache.isReadScriptureEnabled
        rbLock.isChecked = configCache.isLock
        rbHome.isChecked = configCache.isHome
        rbStatic.isChecked = configCache.isLock || configCache.isHome
        config = configCache
        setOneClickBibleColor(configCache.isReadScriptureEnabled)
    }

    private fun setOneClickBibleColor(isChecked: Boolean){
        if(isChecked){
            tvOneClick.setTextColor(ContextCompat.getColor(this,R.color.item_text_normal))
        }else{
            tvOneClick.setTextColor(ContextCompat.getColor(this,R.color.text_gray))
        }
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }

            R.id.confirm -> {
//                if (isPending) {
//                    finish()
//                } else {
//                    isPending = true
                var today = formatDayAgo(0)
                var newConfig = WallpaperConfig(
                    rbDynamic.isChecked,
                    swClickable.isChecked,
                    rbLock.isChecked,
                    rbHome.isChecked,
                    from,
                    "${request.date}-${today}",
                    request.greeting,
                    request.fontUrl
                )
                CardApiService.saveWallPaperConfig(newConfig)
                config = newConfig
                if (rbDynamic.isChecked) {
                    onSetDynamic()
                } else if (rbStatic.isChecked) {
                    onSetStatic()
                }
            }
//            }

        }
    }

    private fun onSetDynamic() {
        WallpaperGuideUtil(this@SettingWallpaperActivity).guideToDirectlySetLiveWallpaper()
    }

    private fun onSetStatic() {
        showProgressDialog("Setting...")
        val disposable = PermissionRequestHandler
            .requestWriteExternalStorage(this@SettingWallpaperActivity)
            .andThen<Bitmap?>( // After permission is granted, execute the following operations
                Observable.create<Bitmap?>(ObservableOnSubscribe { emitter: ObservableEmitter<Bitmap?>? ->
                    CardGenerator.generateBibleCard(
                        applicationContext,
                        R.layout.item_image_greeting,
                        request,
                        false,
                        false,
                        { result: Bitmap? ->
                            emitter!!.onNext(result!!) // 发送成功结果
                            emitter.onComplete() // 完成
                            Unit
                        }, { err: Throwable? ->
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
                        var flags = 0
                        if (config.isHome) {
                            flags = flags or WallpaperManager.FLAG_SYSTEM
                        }
                        if (config.isLock) {
                            flags = flags or WallpaperManager.FLAG_LOCK
                        }
                        // 默认值：如果都没有选中，则两个都设置
                        if (flags == 0) {
                            flags = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        }

                        WallpaperUtils(this@SettingWallpaperActivity).setScreenWallpaper(
                            bitmap,
                            flags
                        )
                    }
                    bitmap?.recycle()
                    dismissProgressDialog()
                },
                Consumer { e: Throwable? ->
                    if (e != null) {
                        onError(e)
                    }
                    dismissProgressDialog()
                }
            )
        dm.add(disposable)
    }

    override fun onCheckedChanged(p0: CompoundButton?, isCheck: Boolean) {
//        isPending = false
        when (p0?.id) {
            R.id.rb_dynamic -> {
                if (isCheck) {
                    if (BillingManager.getInstance()
                            .tryToPay(this@SettingWallpaperActivity, "wallpaper")
                    ) {
                        p0.isChecked = false
                    } else {
                        rbStatic.isChecked = false
                        swClickable.isChecked = true
                        rbDynamic.isChecked = true
                        setOneClickBibleColor(swClickable.isChecked)
                    }
                } else {
                    rbStatic.isChecked = true
                }
            }

            R.id.rb_lock, R.id.rb_home -> {
                rbStatic.isChecked = rbHome.isChecked || rbLock.isChecked
            }

            R.id.switch_read ->{
                setOneClickBibleColor(isCheck)
            }

            R.id.rb_static -> {
                if (isCheck) {
                    if (!rbHome.isChecked && !rbLock.isChecked) {
                        rbHome.isChecked = true
                        rbLock.isChecked = true
                    }
                    rbDynamic.isChecked = false
                    swClickable.isChecked = false
                    setOneClickBibleColor(swClickable.isChecked)
                } else {
                    rbHome.isChecked = false
                    rbLock.isChecked = false
                    if (BillingManager.getInstance().hasSubscriptions()) {
                        rbDynamic.isChecked = true
                        swClickable.isChecked = true
                        setOneClickBibleColor(swClickable.isChecked)
                    }
                }
            }

        }
    }
}