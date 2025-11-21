package sdk.chat.demo.robot.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.GWConfigs
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.handlers.LogUploader
import java.lang.ref.WeakReference

data class FieldValue(
    val field: String,
    var value: String? = null
)

class LabelValueSpinnerAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(context, R.layout.item_input_intent_spinner, items) {
    private var selectedPosition = -1
    private var label = items[0]

    var isUserInteraction: Boolean = false
        get() = field
        set(value) {
            field = value
        }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_input_intent_dropdown, parent, false)

        val textView = view.findViewById<TextView>(R.id.dropdownText)
        if (position == 0) {
            textView.text = context.getString(R.string.deselect)
        } else {
            textView.text = items[position]
        }

        // 设置选中状态
        val selectedView = view.findViewById<View>(R.id.selected)
        if (position == selectedPosition) {
            selectedView.visibility = View.VISIBLE
        } else {
            selectedView.visibility = View.INVISIBLE
        }

        return view
    }

    fun setSelectedPosition(position: Int): String? {
        if (position < 0 || position >= items.size) {
            return null
        }
        selectedPosition = position
        notifyDataSetChanged()
        return items[position];
    }

    fun getSelectedPosition(): Int = selectedPosition


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(R.id.spinnerText)

        // 未选中时只显示label
        if (position == 0) {
            textView.text = "${label}"
        } else {
            textView.text = "${label}:${items[position]}"
        }
        return view
    }

}

class InputIntentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener {
    var mainMenus: WeakReference<GWMsgInput>? = null
    private var menuSong: View
    private lateinit var hideSongMenu: View
    private var containerLayout: LinearLayout
    private var searchCriteria = mutableListOf<FieldValue>()

    fun setMainMenuView(view: GWMsgInput?) {
        mainMenus = if (view != null) WeakReference(view) else null
    }

    fun getHymnsParams(): String? {
        return searchCriteria
            .filter { it.value != null && it.value!!.isNotBlank() }
            .joinToString(", ") { "${it.field}:${it.value}" }
    }

    fun initHymnsParams(configs: GWConfigs) {
        containerLayout.removeAllViews()
        searchCriteria.clear()
        if (configs.hymnsParams != null && !configs.hymnsParams.isEmpty()) {
            configs.hymnsParams.map { param ->
                searchCriteria.add(FieldValue(param.field.toString(), ""))
                val spinner = createCustomSpinner(param)
                containerLayout.addView(spinner)
            }
        }
    }

    fun hideSongMenu() {
        mainMenus?.get()?.onClick(hideSongMenu)
        onHideSongMenu()
    }

    fun onHideSongMenu(){

        menuSong.visibility = GONE
        for (i in 0 until containerLayout.childCount) {
            val child = containerLayout.getChildAt(i)
            // 检查是否为 Spinner
            if (child is Spinner) {
//                        Log.e("inputintent","clear.setSelectedPosition 0")
                (child.adapter as LabelValueSpinnerAdapter).isUserInteraction = false
                child.setSelection(0, false)
            }
        }
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.hymns -> {
                menuSong.visibility = VISIBLE
                true
            }

            R.id.hideSongMenu -> {
                mainMenus?.get()?.onClick(view)
                onHideSongMenu()
                LogUploader.reportEvent(
                    "mod_chat", listOf<KeyValuePair?>(
                        KeyValuePair("chat_action", "63")
                    )
                )
                true
            }
        }
    }

    init {
        // 加载布局
        inflate(context, R.layout.item_input_intent, this)
        menuSong = findViewById(R.id.menuSong)
        containerLayout = findViewById(R.id.containerLayout)
        hideSongMenu = findViewById<View?>(R.id.hideSongMenu)
        hideSongMenu.setOnClickListener(this)
    }

//    private fun adjustSpinnerWidth(spinner: Spinner, hymnParam: GWConfigs.HymnParam) {
//        // 计算最长的文本（包括label和所有选项）
//        var paint: Paint
//        val textView = spinner.findViewById<TextView>(android.R.id.text1)
//        if (textView != null) {
//            paint = textView.paint
//        } else {
//            // 备用方案：创建新的 Paint 对象
//            paint = Paint().apply {
//                textSize = 14.spToPx() // 设置与 Spinner 相同的字体大小
//                typeface = Typeface.DEFAULT
//            }
//        }
//
////        val allTexts = listOf(hymnParam.field) + hymnParam.choices
////        val maxTextWidth = allTexts.maxOf { text ->
////            paint.measureText(text)
//
//        val maxTextWidth = 0
//        // 计算总宽度（文本宽度 + 内边距 + 箭头区域）
//        val padding = spinner.paddingLeft + spinner.paddingRight
//        val arrowArea = 48.dpToPx()
//        val minWidth = 60.dpToPx()
//        val maxWidth = (resources.displayMetrics.widthPixels * 0.3).toInt()
//
//        val targetWidth = (maxTextWidth + padding + arrowArea + 150).toInt()
//            .coerceAtLeast(minWidth)
//            .coerceAtMost(maxWidth)
//
//        // 应用宽度调整
//        spinner.layoutParams = spinner.layoutParams.apply {
//            width = targetWidth
//        }
//        spinner.requestLayout()
//    }

    private fun createCustomSpinner(hymnParam: GWConfigs.HymnParam): Spinner {
        // 创建自定义下拉框
        val spinner = Spinner(this.context, Spinner.MODE_DROPDOWN).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16.dpToPx() // 设置右边距
                minimumWidth = 98.dpToPx()
                setBackgroundResource(R.drawable.spinner_bg_oval)
            }

            val list = mutableListOf<String>()
            hymnParam.field?.let { list.add(it) }
            hymnParam.choices?.forEach { list.add(it) }

            adapter =
                LabelValueSpinnerAdapter(this.context, list)

            // 下拉框点击事件
            val listener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // 处理选项选择
//                    Log.e("inputintent","OnItemSelectedListener $position")
                    val fieldValue = searchCriteria.find { it.field == hymnParam.field }
                    val selected =
                        (adapter as LabelValueSpinnerAdapter).setSelectedPosition(position)
                    fieldValue?.value = if (position == 0) null else selected

                    if ((adapter as LabelValueSpinnerAdapter).isUserInteraction) {
                        LogUploader.reportEvent(
                            "mod_chat", listOf<KeyValuePair?>(
                                KeyValuePair("chat_action", "62")
                            )
                        )
                    }
//                    LogUploader.reportEvent(
//                        "mod_chat", listOf<KeyValuePair?>(
//                            KeyValuePair("chat_action", "62")
//                        )
//                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            onItemSelectedListener = listener
            // 动态调整宽度
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    (adapter as LabelValueSpinnerAdapter).isUserInteraction = true

                    LogUploader.reportEvent(
                        "mod_chat", listOf<KeyValuePair?>(
                            KeyValuePair("chat_action", "61")
                        )
                    )
                }
                false // 不消费事件，让 Spinner 正常处理
            }
        }

        return spinner
    }

    // 扩展函数：dp转px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // 扩展函数：sp转px
    private fun Int.spToPx(): Float = this * resources.displayMetrics.scaledDensity
}