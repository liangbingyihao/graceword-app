package sdk.chat.demo.robot.ui

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.RenderProps
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.tag.SimpleTagHandler
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.activities.BibleActivity
import sdk.chat.demo.robot.handlers.BibleApiService
import sdk.chat.demo.robot.utils.ToastHelper
import java.util.Collections

class RedUnderlineTagHandler : SimpleTagHandler() {

    // 自定义的点击事件接口
    interface OnUnderlineClickListener {
        fun onUnderlineClick(widget: View, text: String)
    }

    // 默认的点击事件处理器
    private var defaultClickListener: OnUnderlineClickListener = object : OnUnderlineClickListener {
        override fun onUnderlineClick(widget: View, text: String) {
            BibleActivity.start(widget.context,text)
//            BibleApiService.getInstance().getChapter("1", 0, text) { chapter ->
//                if (chapter != null) {
//                    // 更新当前章节信息
////                    currentBookId = bookId
////                    currentChapterNumber = chapterNumber
////
////                    // 更新UI
////                    updateChapterUI(chapter)
//                } else {
//                    // 显示错误
////                    showError()
//                }
//
//                // 隐藏加载中
////                hideLoading()
//            }
        }
    }

    // 当前的点击事件处理器
    private var currentClickListener: OnUnderlineClickListener = defaultClickListener

    // 设置自定义点击事件
    fun setOnUnderlineClickListener(listener: OnUnderlineClickListener) {
        currentClickListener = listener ?: defaultClickListener
    }

    override fun getSpans(
        configuration: MarkwonConfiguration,
        renderProps: RenderProps,
        tag: HtmlTag
    ): Any? {
        // 检查是否包含目标class
        val classAttribute = tag.attributes().get("class")
        val hasTargetClass = classAttribute?.split(" ")?.contains(TARGET_CLASS) == true

        // 如果不包含目标class，返回null，使用默认处理
        if (!hasTargetClass) {
            return null
        }
        // 返回需要应用的span
        return arrayOf(
//            ForegroundColorSpan(Color.RED),
            CustomDashedUnderlineSpan(0xFFCF4B40.toInt())
        )
    }

    override fun supportedTags(): Collection<String?> {
        return Collections.singleton(TAG_NAME);
    }

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        // 检查是否包含目标class
        val classAttribute = tag.attributes().get("class")
        val hasTargetClass = classAttribute?.split(" ")?.contains(TARGET_CLASS) == true

        // 如果不包含目标class，不处理，使用默认行为
        if (!hasTargetClass) {
            return
        }

        // 调用父类的handle方法，应用getSpans方法返回的span
        super.handle(visitor, renderer, tag)

        // 获取标签内容的范围
        val start = tag.start()
        val end = tag.end()

        if (start < end && end <= visitor.builder().length) {
            // 获取标签内容
            val text = visitor.builder().subSequence(start, end).toString()

            // 设置点击事件
            visitor.builder().setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        currentClickListener.onUnderlineClick(widget, text)
                    }
                },
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    companion object {
        // 标签名称
        const val TAG_NAME = "u"
        const val TARGET_CLASS = "bible"
    }
}

