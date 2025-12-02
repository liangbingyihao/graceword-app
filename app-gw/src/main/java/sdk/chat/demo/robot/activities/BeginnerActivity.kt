package sdk.chat.demo.robot.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.gyf.immersionbar.ImmersionBar
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.extensions.dpToPx
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.holder.WelcomeHolder

class BeginnerActivity : BaseActivity() {
    private lateinit var buttonContainer: LinearLayout
    private val buttonList = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).init()
        setContentView(R.layout.activity_beginner)
        buttonContainer = findViewById(R.id.button_container)

        val photoView: ImageView = findViewById<ImageView>(R.id.photoView)


        var txContent: TextView = findViewById(R.id.question)
        var txTitle: TextView = findViewById(R.id.title)
        var configs = ImageApi.getGwConfigs()
        if (configs == null || configs.welcomeSurvey == null) {
            finish()
        } else {
            Glide.with(this@BeginnerActivity)
                .load(configs.welcomeSurvey.background)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                .placeholder(R.mipmap.bg_beginner) // 占位图
                .error(R.mipmap.bg_beginner) // 错误图
                .into(photoView)


            var welcomeSurvey = configs.welcomeSurvey
            txContent.text = welcomeSurvey.question
            txTitle.text = welcomeSurvey.title
            val valueList: List<String> = welcomeSurvey.options
                ?.mapNotNull { it.value } // 返回 List<String>
                ?: emptyList() // 返回空列表
            addDynamicMaterialButtons(valueList)
        }
        LogUploader.reportEvent(
            "mod_guide", listOf<KeyValuePair?>(
                KeyValuePair("guide_action", "0"),
                KeyValuePair("guide_type", "new_launch")
            )
        )
    }

    override fun getLayout(): Int {
        return 0
    }

    /**
     * 动态添加 MaterialButton
     */
    private fun addDynamicMaterialButtons(buttonTexts: List<String>) {
        buttonContainer.removeAllViews()
        buttonList.clear()

        if (buttonTexts.isEmpty()) return

//        // 计算可用高度（减去底部按钮和边距）
//        val availableHeight = calculateAvailableHeight()
//        val buttonHeight = availableHeight/buttonTexts.size
        val uniformWidth = calculateMaxButtonWidth(buttonTexts)

        // 添加动态按钮
        buttonTexts.forEachIndexed { index, text ->
            val button = createMaterialButton(text, index, uniformWidth)
            buttonContainer.addView(button) // 添加到顶部，确保对话按钮在底部
            buttonList.add(button)
        }

    }

    private fun calculateMaxButtonWidth(buttonTexts: List<String>): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val maxAvailableWidth = screenWidth - 20.dpToPx(this)

        return buttonTexts.maxOfOrNull { text ->
            val tempButton = MaterialButton(this).apply {
                setText(text)
                textSize = 14f
                setPadding(
                    16.dpToPx(this@BeginnerActivity), 8.dpToPx(this@BeginnerActivity),
                    16.dpToPx(this@BeginnerActivity), 8.dpToPx(this@BeginnerActivity)
                )
            }

            tempButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            tempButton.measuredWidth
        }?.coerceAtMost(maxAvailableWidth) ?: maxAvailableWidth
    }

    private fun createMaterialButton(text: String, index: Int, uniformWidth: Int): MaterialButton {
        return MaterialButton(this).apply {// 计算最大宽度（屏幕宽度 - 两边各48dp边距）
//            val screenWidth = resources.displayMetrics.widthPixels
//            val maxWidth = screenWidth - 24.dpToPx(context)

            // 设置布局参数
            layoutParams = LinearLayout.LayoutParams(
                uniformWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) {
                    topMargin = 8.dpToPx(this@BeginnerActivity)
                }
            }

//            // 设置最大宽度
//            this.maxWidth = maxWidth

            // 设置背景和边框
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            isAllCaps = false
            textSize = 14f

            // 设置 Material Design 属性
            cornerRadius = 21.dpToPx(context)
            strokeColor = ColorStateList.valueOf(Color.WHITE)
            strokeWidth = 1.dpToPx(context)

            // 设置按钮属性
            this.text = text
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            // 设置内边距
            val horizontalPadding = 16.dpToPx(context)
            val verticalPadding = 8.dpToPx(context)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

            // 设置文本显示属性
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            isSingleLine = false // 允许多行显示

            // 设置点击效果
            setOnClickListener {
                onMaterialButtonClick(index, text)
            }

            // 添加波纹效果
            isClickable = true
            isFocusable = true
        }
    }

    private fun onMaterialButtonClick(index: Int, text: String) {
        animateButtonClick(buttonList[index])
//        ToastHelper.show(this@BeginnerActivity, text)
        ChatSDK.events().source()
            .accept(NetworkEvent.messageAdded(null, WelcomeHolder.getWelcomeMessage(text)))
        LogUploader.reportEvent(
            "mod_guide", listOf<KeyValuePair?>(
                KeyValuePair("guide_action", "30"),
                KeyValuePair("guide_option_id", (index + 1).toString())
            )
        )
        finish()
    }

    private fun animateButtonClick(button: MaterialButton) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

}