package sdk.chat.demo.robot.ui
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.utils.AdvancedChineseEnglishFilter

/**
 * 支持中英文限制的 EditText
 */
class TextLimitEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    // 限制配置
    private var maxChineseChars: Int = 50
    private var maxEnglishWords: Int = 20

    // 计数器视图
    private var counterView: TextView? = null
    private var showCounter: Boolean = true

    // 监听器
    private var onCountChangeListener: OnCountChangeListener? = null

    interface OnCountChangeListener {
        fun onChineseCountChange(count: Int, max: Int)
        fun onEnglishCountChange(count: Int, max: Int)
        fun onLimitReached(isChineseLimit: Boolean, isEnglishLimit: Boolean)
    }

    init {
        initAttributes(attrs)
        setupTextWatcher()
        applyFilter()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextLimitEditText)

        maxChineseChars = typedArray.getInt(R.styleable.TextLimitEditText_maxChineseChars, 50)
        maxEnglishWords = typedArray.getInt(R.styleable.TextLimitEditText_maxEnglishWords, 20)
        showCounter = typedArray.getBoolean(R.styleable.TextLimitEditText_showCounter, true)

        typedArray.recycle()
    }

    private fun setupTextWatcher() {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                updateCounter(text)
                notifyCountChange(text)
            }
        })
    }

    private fun applyFilter() {
        val filter = AdvancedChineseEnglishFilter(
            maxTotalCount = maxChineseChars,
        ) { totalCount ->
            updateCounterState(1, 2)
        }

        filters = arrayOf(filter)
    }

    /**
     * 更新计数器显示
     */
    private fun updateCounter(text: String) {
        counterView?.let { counter ->
            val chineseCount = countChineseCharacters(text)
            val englishCount = countEnglishWords(text)

            val counterText = buildString {
                append("中文: $chineseCount/$maxChineseChars")
                append(" | ")
                append("英文: $englishCount/$maxEnglishWords")
            }

            counter.text = counterText

            // 设置颜色
            val colorRes = when {
                chineseCount > maxChineseChars || englishCount > maxEnglishWords ->
                    android.R.color.holo_red_dark
                chineseCount > maxChineseChars * 0.8 || englishCount > maxEnglishWords * 0.8 ->
                    android.R.color.holo_orange_dark
                else -> android.R.color.darker_gray
            }

            counter.setTextColor(ContextCompat.getColor(context, colorRes))
        }
    }

    /**
     * 更新限制状态
     */
    private fun updateCounterState(chineseCount: Int, englishCount: Int) {
        val isChineseLimit = chineseCount >= maxChineseChars
        val isEnglishLimit = englishCount >= maxEnglishWords

        if (isChineseLimit || isEnglishLimit) {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            onCountChangeListener?.onLimitReached(isChineseLimit, isEnglishLimit)
        } else {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
    }

    /**
     * 通知计数变化
     */
    private fun notifyCountChange(text: String) {
        val chineseCount = countChineseCharacters(text)
        val englishCount = countEnglishWords(text)

        onCountChangeListener?.onChineseCountChange(chineseCount, maxChineseChars)
        onCountChangeListener?.onEnglishCountChange(englishCount, maxEnglishWords)
    }

    /**
     * 设置计数器视图
     */
    fun setCounterView(view: TextView) {
        this.counterView = view
        updateCounter(text?.toString() ?: "")
    }

    /**
     * 设置限制
     */
    fun setLimits(chineseChars: Int, englishWords: Int) {
        this.maxChineseChars = chineseChars
        this.maxEnglishWords = englishWords
        applyFilter()
        updateCounter(text?.toString() ?: "")
    }

    /**
     * 获取当前计数
     */
    fun getCurrentCounts(): Pair<Int, Int> {
        val text = text?.toString() ?: ""
        return Pair(countChineseCharacters(text), countEnglishWords(text))
    }

    /**
     * 检查是否超出限制
     */
    fun isExceedLimit(): Boolean {
        val (chineseCount, englishCount) = getCurrentCounts()
        return chineseCount > maxChineseChars || englishCount > maxEnglishWords
    }

    /**
     * 清空文本
     */
    fun clear() {
        setText("")
    }

    companion object {
        fun countChineseCharacters(text: String): Int {
            return text.count { it in '\u4e00'..'\u9fff' }
        }

        fun countEnglishWords(text: String): Int {
            return text.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
        }
    }
}