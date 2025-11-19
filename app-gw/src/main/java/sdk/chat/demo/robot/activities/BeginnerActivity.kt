package sdk.chat.demo.robot.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
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
import sdk.chat.demo.robot.utils.ToastHelper

class BeginnerActivity : BaseActivity() {
    private lateinit var buttonContainer: LinearLayout
    private val buttonList = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).init()
        setContentView(R.layout.activity_beginner)
        buttonContainer = findViewById(R.id.button_container)

        var txContent: TextView = findViewById(R.id.question)
        var txTitle: TextView = findViewById(R.id.title)
        var configs = ImageApi.getGwConfigs()
        if (configs == null || configs.welcomeSurvey == null) {
            finish()
        } else {
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

        // 添加动态按钮
        buttonTexts.forEachIndexed { index, text ->
            val button = createMaterialButton(text, index)
            buttonContainer.addView(button) // 添加到顶部，确保对话按钮在底部
            buttonList.add(button)
        }

    }

    /**
     * 计算可用高度（总高度 - 底部按钮高度 - 边距）
     */
    private fun calculateAvailableHeight(): Int {
        return if (buttonContainer.measuredHeight > 0) {
            val totalHeight = buttonContainer.measuredHeight
            val bottomButtonHeight = 42.dpToPx(this) // 按钮高度
            val bottomMargin = 64.dpToPx(this)      // 底部边距
            totalHeight - bottomButtonHeight - bottomMargin
        } else {
            // 使用屏幕高度估算
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            (screenHeight * 0.3).toInt() // 预留30%给底部按钮
        }
    }

    private fun createMaterialButton(text: String, index: Int): MaterialButton {
        return MaterialButton(this).apply {
            // 设置布局参数
            layoutParams = LinearLayout.LayoutParams(
                240.dpToPx(context),
                42.dpToPx(context)
            ).apply {
                if (index > 0) {
                    topMargin = 8.dpToPx(this@BeginnerActivity)
                }
            }
            // 设置背景和边框
            backgroundTintList =
                ColorStateList.valueOf(Color.TRANSPARENT) // android:backgroundTint="@android:color/transparent"
            isAllCaps =
                false                                               // android:textAllCaps="false"
            textSize =
                14f                                                  // android:textSize="14sp"

            // 设置 Material Design 属性
            cornerRadius = 21.dpToPx(context)                             // app:cornerRadius="21dp"
            strokeColor =
                ColorStateList.valueOf(Color.WHITE)               // app:strokeColor="#FFFFFF"
            strokeWidth = 1.dpToPx(context)                                // app:strokeWidth="1dp"

            // 设置按钮属性（保持与对话按钮一致的样式）
            this.text = text
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

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
        ChatSDK.events().source().accept(NetworkEvent.messageAdded(null,WelcomeHolder.getWelcomeMessage(text)))
        LogUploader.reportEvent(
            "mod_guide", listOf<KeyValuePair?>(
                KeyValuePair("guide_action", "30"),
                KeyValuePair("guide_option_id", (index+1).toString())
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