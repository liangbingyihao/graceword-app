package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.ui.listener.GWClickListener


class ChatActivity : BaseActivity(), View.OnClickListener,
    GWClickListener.TTSSpeaker {
    private val chatTag = "tag_chat";
    private var from: String? = null
//    private var textToSpeech: TextToSpeech? = null
//    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"
        private const val EXTRA_CHAT_FROM = "chat_from"
        private const val EXTRA_INPUT = "input"

        // 提供静态启动方法（推荐）
        fun start(context: Context?, messageId: String? = null, from: String? = null,input: String? = null) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, messageId)
                putExtra(EXTRA_CHAT_FROM, from)
                putExtra(EXTRA_INPUT, input)
            }
            context?.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        val messageId = intent.getStringExtra(EXTRA_INITIAL_DATA)
        val input = intent.getStringExtra(EXTRA_INPUT)
        from = intent.getStringExtra(EXTRA_CHAT_FROM)
        if (from != null && !from!!.isEmpty()) {
            LogUploader.chatEntrance(from)
        }

// 设置参数
        val fragment = GWChatFragment().apply {
            arguments = bundleOf(
                "KEY_MESSAGE_ID" to messageId,
                "KEY_INPUT" to input,
            )
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, chatTag).commit()


        findViewById<View?>(R.id.home).setOnClickListener(this)

    }

    private fun safeInstallTtsEngine() {
        val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 再次检查安装 Intent 是否可用
        if (installIntent.resolveActivity(packageManager) != null) {
            startActivity(installIntent)
        } else {
            // 连安装入口都没有的极端情况（如国产 ROM）
            Toast.makeText(
                this,
                "您的设备不支持语音功能",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun speek(text: String, msgId: String) {
        TTSHelper.speek(text, msgId)
    }

    override fun getCurrentUtteranceId(): String? {
        return currentUtteranceId;
    }

    override fun stop() {
        TTSHelper.stop()
    }


    override fun getLayout(): Int {
        return R.layout.activity_chat;
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.home) {
            finish();
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (from != null && !from!!.isEmpty()) {
            LogUploader.chatExit()
        }
//        TTSHelper.clear()
    }
}