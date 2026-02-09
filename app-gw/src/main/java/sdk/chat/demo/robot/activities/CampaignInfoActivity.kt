package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import io.reactivex.functions.Consumer
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.Campaign
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.LogUploader

class CampaignInfoActivity : BaseActivity() {
    companion object {
        private const val ARG_MODE = "popup_mode"
        const val MODE_TASK = "TASK"
        const val MODE_MAIN = "main"
        const val MODE_MINI = "mini"

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

    private var popupMode: String? = MODE_MINI
//    private lateinit var bgMain: MaxWidthWrapImageView
//    private lateinit var bgPos: MaxWidthWrapImageView
    private lateinit var bgMain: ImageView
    private lateinit var bgPos: View
    private lateinit var txExit: View
    private var configData: Campaign? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        popupMode = intent.getStringExtra(ARG_MODE)
        var resId = R.layout.dialog_campaign_mini
        if(popupMode==MODE_TASK){
            resId = R.layout.dialog_task_done
        }else if(popupMode==MODE_MAIN){
            resId = R.layout.dialog_campaign_main
        }
        setContentView(resId)
        bgMain = findViewById<ImageView>(R.id.bg_main)
        bgPos = findViewById<View>(R.id.positive)
        txExit = findViewById<View>(R.id.exit)
        txExit.setOnClickListener {
            if(MODE_TASK != popupMode){
                LogUploader.reportEvent(
                    "mod_activity", listOf<KeyValuePair?>(
                        KeyValuePair("activity_action", "10"),
                        KeyValuePair("activity_page_type", "update"),
                    )
                )
            }
            finish()
        }
        bgPos.setOnClickListener {
            if(MODE_TASK == popupMode){
                // 这里是positiveAction的逻辑
                startActivity(Intent(this@CampaignInfoActivity, TaskActivity::class.java))
            }else{
                CardApiService.handleJoinCampaign(this@CampaignInfoActivity)

                LogUploader.reportEvent(
                    "mod_activity", listOf<KeyValuePair?>(
                        KeyValuePair("activity_action", "20"),
                        KeyValuePair("activity_page_type", "update"),
                    )
                )
            }
            finish()
        }

        if(MODE_TASK != popupMode) {
            LogUploader.reportEvent(
                "mod_activity", listOf<KeyValuePair?>(
                    KeyValuePair("activity_action", "0"),
                    KeyValuePair("activity_page_type", "update"),
                )
            )
            loadData()
        }
    }

    fun loadData() {
        dm.add(
            CardApiService.getCampaignData().subscribe(
                { data ->
                    if (data != null) {
                        configData = data
                        if(popupMode==MODE_MAIN){
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
                .into(bgPos as ImageView)
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
                .into(bgPos as ImageView)

            with(txExit as TextView) {
                setTextColor(data.dailyPopupConfig.dismissButtonTextColor.toColorInt())
                text = data.dailyPopupConfig.dismissButtonText
            }
        }
    }
}