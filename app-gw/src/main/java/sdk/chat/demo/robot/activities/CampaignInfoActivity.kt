package sdk.chat.demo.robot.activities

import android.view.View
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import io.reactivex.functions.Consumer
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.handlers.CardApiService
import com.bumptech.glide.request.RequestListener
import sdk.chat.demo.robot.api.model.Campaign
import sdk.chat.demo.robot.ui.MaxWidthWrapImageView
import androidx.core.graphics.toColorInt

class CampaignInfoActivity : BaseActivity() {
    companion object {
        private const val ARG_MODE = "popup_mode"

        // 提供静态启动方法（推荐）
        fun start(
            context: Context,
            mode: String
        ) {
            val intent = Intent(context, CampaignInfoActivity::class.java).apply {
                putExtra(ARG_MODE, mode)
            }
            context.startActivity(intent)
        }
    }

    override fun getLayout(): Int {
        return 0;
    }

    private var popupMode: String = "mini"
//    private lateinit var bgMain: MaxWidthWrapImageView
//    private lateinit var bgPos: MaxWidthWrapImageView
    private lateinit var bgMain: ImageView
    private lateinit var bgPos: ImageView
    private lateinit var txExit: View
    private var configData: Campaign? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        var mode = intent.getStringExtra(ARG_MODE)
        var resId = R.layout.dialog_campaign_mini
        if (!mode.isNullOrEmpty()) {
            if (mode == "main") {
                popupMode = mode
                resId = R.layout.dialog_campaign_main
            }
        }
        setContentView(resId)
        bgMain = findViewById<ImageView>(R.id.bg_main)
        bgPos = findViewById<ImageView>(R.id.positive)
        txExit = findViewById<View>(R.id.exit)
        txExit.setOnClickListener { finish() }
        bgPos.setOnClickListener {
            CardApiService.handleJoinCampaign(this@CampaignInfoActivity)
            finish()
        }

        loadData()
    }

    fun loadData() {
        dm.add(
            CardApiService.getCampaignData().subscribe(
                { data ->
                    if (data != null) {
                        configData = data
                        if(popupMode=="main"){
                            setMainView()
                        }else{
                            setMinView()
                        }
                    }
                },
                Consumer { e: Throwable? ->
//                    ToastHelper.show(this@EditCardActivity, e.toString())
                })
        )
    }

    private fun setMainView() {
        var data = configData
        if (data != null && data.popupConfig != null && data.popupConfig.enable) {
            Glide.with(this@CampaignInfoActivity)
                .load(data.popupConfig.backgroundUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        finish()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        CardApiService.finishLaunchOAMain(this@CampaignInfoActivity)
                        return false
                    }
                })
                .into(bgMain)

            Glide.with(this@CampaignInfoActivity)
                .load(data.popupConfig.buttonUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.mipmap.bg_campaign_main_y)
//                                .override(maxWidth * 2 / 3)
                .into(bgPos)
        }
    }


    private fun setMinView() {
        var data = configData
        if (data != null && data.dailyPopupConfig != null && data.dailyPopupConfig.enable) {
            Glide.with(this@CampaignInfoActivity)
                .load(data.dailyPopupConfig.backgroundUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        finish()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        CardApiService.finishLaunchOAMain(this@CampaignInfoActivity)
                        return false
                    }
                })
                .into(bgMain)

            Glide.with(this@CampaignInfoActivity)
                .load(data.dailyPopupConfig.buttonUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.mipmap.bg_campaign_main_y)
                .into(bgPos)

            with(txExit as TextView) {
                setTextColor(data.dailyPopupConfig.dismissButtonTextColor.toColorInt())
                text = data.dailyPopupConfig.dismissButtonText
            }
        }
    }
}