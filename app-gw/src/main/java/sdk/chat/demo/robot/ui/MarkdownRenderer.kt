package sdk.chat.demo.robot.ui

import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import sdk.chat.demo.MainApp

object MarkdownRenderer {
    val markwon: Markwon by lazy {
        Markwon.builder(MainApp.getContext())
            .usePlugin(HtmlPlugin.create().addHandler(RedUnderlineTagHandler()))
            .build()
    }
}