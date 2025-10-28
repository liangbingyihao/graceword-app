package sdk.chat.demo.robot.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import sdk.chat.core.dao.Message
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.BaseActivity
import java.lang.ref.WeakReference


class InputIntentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener {
    private var weakContext: WeakReference<BaseActivity>? = null
    private var weakMessage: WeakReference<Message>? = null
//    private val maskBackground: View
//    private val highlightIndicator: View
//风格、主题、场合、乐团 Style, Theme, Occasion, Orchestra
    private var spStyle: Spinner
    private var spTheme: Spinner
    private var spOccasion: Spinner
    private var spOrchestra: Spinner
    private var menuSong: View
    private var menus: View
    private var mode: String? = null

    private val guideDrawer = "guide_drawer"
    private val guidePic = "guide_pic"
    private val guidePray = "guide_pray"
    private val allModes = arrayOf(guidePic, guidePray, guideDrawer)
    // 下拉框选项数据
    private val spinnerItems = listOf(
        "短选项",
        "中等长度的选项内容",
        "这是一个非常非常长的选项内容，需要动态调整宽度",
        "另一个长选项：Android开发中的自定义视图实现",
        "短"
    )


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.hymns -> {
                menuSong.visibility = VISIBLE
                menus.visibility = GONE
                true
            }

            R.id.hideSongMenu -> {
                menuSong.visibility = GONE
                menus.visibility = VISIBLE
                true
            }
        }
    }

    init {
        // 加载布局
        inflate(context, R.layout.item_input_intent, this)
        spStyle = findViewById(R.id.spStyle)
        spTheme = findViewById(R.id.spTheme)
        spOccasion = findViewById(R.id.spOccasion)
        spOrchestra = findViewById(R.id.spOrchestra)
        menuSong =  findViewById(R.id.menuSong)
        menus =  findViewById(R.id.menus)

        spStyle.adapter = ArrayAdapter<String>(
            this.context,
            R.layout.item_input_intent_spinner,
            spinnerItems
        )
        spTheme.adapter = ArrayAdapter<String>(
            this.context,
            R.layout.item_input_intent_spinner,
            spinnerItems
        )
        spOccasion.adapter = ArrayAdapter<String>(
            this.context,
            R.layout.item_input_intent_spinner,
            spinnerItems
        )
        spOrchestra.adapter = ArrayAdapter<String>(
            this.context,
            R.layout.item_input_intent_spinner,
            spinnerItems
        )
        //                circleOverlay.startAnimation()
//        attachmentButtonSpace = findViewById(R.id.attachmentButtonSpace);
        findViewById<View?>(R.id.hymns).setOnClickListener(this)
        findViewById<View?>(R.id.bible).setOnClickListener(this)
        findViewById<View?>(R.id.hideSongMenu).setOnClickListener(this)

//        highlightIndicator = findViewById(R.id.guide_view)
//        highlightTarget = findViewById(R.id.highlight_target)
//        highlightDesc = findViewById(R.id.highlight_desc)
//        maskBackground.setOnClickListener(onClickListener)
//        findViewById<View>(R.id.btn_next).setOnClickListener(onClickListener)
    }

}