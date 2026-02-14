package sdk.chat.demo.robot.ui
import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView

class ExpandableTextViewHelper2(
    private val textView: TextView,
    private val maxLinesCollapsed: Int = 3,
    private val expandText: String = "展开",
    private val collapseText: String = "收起",
    private val linkColor: Int = Color.BLUE // 默认蓝色
) {

    private var originalText: CharSequence = ""
    private var isExpanded = false

    // 使用对象表达式创建监听器
    private val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            // 移除监听器，避免重复调用
            textView.viewTreeObserver.removeOnPreDrawListener(this)

            // 确保布局完成
            if (textView.lineCount > maxLinesCollapsed) {
                setupExpandableText()
            } else {
                textView.text = originalText
            }
            return true
        }
    }

    // 构造函数重载
    constructor(
        textView: TextView,
        maxLinesCollapsed: Int = 3,
        expandText: String = "展开",
        collapseText: String = "收起"
    ) : this(
        textView,
        maxLinesCollapsed,
        expandText,
        collapseText,
        getDefaultLinkColor(textView.context)
    )

    companion object {
        private fun getDefaultLinkColor(context: Context): Int {
            return try {
                // 尝试使用 colorAccent
//                ContextCompat.getColor(context, R.color.colorAccent)
                Color.parseColor("#1A73E8")
            } catch (e: Exception) {
                // 如果未定义，使用蓝色
                Color.parseColor("#1A73E8")
            }
        }
    }

    fun setText(text: CharSequence) {
        originalText = text
        textView.text = text

        // 重置状态
        isExpanded = false
        textView.maxLines = Integer.MAX_VALUE

        // 设置文本后延迟检查
        textView.post {
            setupTextView()
        }
    }

    private fun setupTextView() {
        // 确保移除旧的监听器
        try {
            textView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        } catch (e: Exception) {
            // 忽略异常
        }

        // 如果文本可能很长，添加监听器
        if (originalText.length > 50) { // 阈值可以根据需要调整
            textView.viewTreeObserver.addOnPreDrawListener(preDrawListener)
        } else {
            // 短文本直接设置
            textView.text = originalText
        }
    }

    private fun setupExpandableText() {
        if (isExpanded) {
            // 展开状态
            textView.text = buildExpandedText()
            textView.maxLines = Integer.MAX_VALUE
        } else {
            // 收起状态
            val layout = textView.layout ?: return

            // 计算截断位置
            val lineEndIndex = maxLinesCollapsed - 1
            if (lineEndIndex >= layout.lineCount) {
                textView.text = originalText
                return
            }

            val lineEnd = layout.getLineEnd(lineEndIndex)
            // 确保不越界
            val safeEnd = lineEnd.coerceAtMost(originalText.length)

            // 计算需要保留的文本长度
            val ellipsisText = "... $expandText"
            val availableWidth = layout.width
            val paint = textView.paint

            // 找到合适的截断位置
            var truncatedLength = safeEnd
            while (truncatedLength > 0) {
                val testText = originalText.subSequence(0, truncatedLength).toString() + ellipsisText
                if (paint.measureText(testText) <= availableWidth) {
                    break
                }
                truncatedLength--
            }

            if (truncatedLength <= 0) {
                textView.text = originalText
                return
            }

            val truncatedText = originalText.subSequence(0, truncatedLength)
            val spannable = SpannableString("$truncatedText$ellipsisText")

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    toggleExpand()
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                    ds.color = linkColor
                }
            }

            // 设置可点击区域
            val start = truncatedText.length + 4 // "... ".length
            val end = spannable.length

            if (start < end) {
                spannable.setSpan(
                    clickableSpan,
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            textView.text = spannable
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.maxLines = maxLinesCollapsed
        }
    }

    private fun buildExpandedText(): SpannableString {
        val spannable = SpannableString("$originalText $collapseText")

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                toggleExpand()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
                ds.color = linkColor
            }
        }

        val start = originalText.length + 1
        val end = spannable.length

        if (start < end) {
            spannable.setSpan(
                clickableSpan,
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        setupExpandableText()
    }

    fun isExpanded(): Boolean = isExpanded

    fun dispose() {
        try {
            textView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        } catch (e: Exception) {
            // 忽略异常
        }
    }
}