package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.RadioButton
import android.widget.Switch
import com.google.gson.Gson
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.SettingsActivity
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.WallpaperConfig


class SettingWallpaperActivity : BaseActivity(), View.OnClickListener, OnCheckedChangeListener {
    private lateinit var rbDynamic: CheckBox
    private lateinit var rbStatic: CheckBox
    private lateinit var rbHome: CheckBox
    private lateinit var rbLock: CheckBox
    private lateinit var swClickable: Switch
    private var from: String = ""

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

        rbDynamic.setOnCheckedChangeListener(this)
        rbHome.setOnCheckedChangeListener(this)
        rbLock.setOnCheckedChangeListener(this)
        rbStatic.setOnCheckedChangeListener(this)


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
        var config = CardApiService.getWallPaperConfig();
        if (config != null) {
            rbDynamic.isChecked = config.isDynamic
            swClickable.isChecked = config.isReadScriptureEnabled
            rbLock.isChecked = config.isLock
            rbHome.isChecked = config.isHome
            rbStatic.isChecked = config.isLock || config.isHome
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
                var config = WallpaperConfig(
                    rbDynamic.isChecked,
                    swClickable.isChecked, rbLock.isChecked, rbHome.isChecked, from, request.date
                )
                CardApiService.saveWallPaperConfig(config)
            }

        }
    }

    override fun onCheckedChanged(p0: CompoundButton?, isCheck: Boolean) {
        when (p0?.id) {
            R.id.rb_dynamic -> {
                if (isCheck) {
                    if (BillingManager.getInstance()
                            .tryToPay(this@SettingWallpaperActivity, "wallpaper")
                    ) {
                        p0.isChecked = false
                    } else {
                        rbStatic.isChecked = false
                    }
                } else {
                    rbStatic.isChecked = true
                }
            }

            R.id.rb_lock, R.id.rb_home -> {
                rbStatic.isChecked = rbHome.isChecked || rbLock.isChecked
            }

            R.id.rb_static -> {
                if (isCheck) {
                    if (!rbHome.isChecked && !rbLock.isChecked) {
                        rbHome.isChecked = true
                        rbLock.isChecked = true
                    }
                    rbDynamic.isChecked = false
                } else {
                    rbHome.isChecked = false
                    rbLock.isChecked = false
                    if (BillingManager.getInstance().hasSubscriptions()) {
                        rbDynamic.isChecked = true
                    }
                }
            }

        }
    }
}