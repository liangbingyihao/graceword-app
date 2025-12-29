package sdk.chat.demo.robot.dialog

import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import sdk.chat.demo.pre.R
import androidx.core.graphics.drawable.toDrawable
import sdk.chat.demo.MainApp

class DialogBibleGuide : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. 加载自定义布局
        return inflater.inflate(R.layout.overlay_bible, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 3. 设置点击事件
        view.findViewById<View>(R.id.mask_background).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btn_next).setOnClickListener {
            dismiss()
        }

        context?.getSharedPreferences("app_prefs", MODE_PRIVATE)
            ?.edit() {
                putBoolean("has_shown_guide_bible", true)
            }
    }

    override fun onStart() {
        super.onStart()
        // 5. 设置 Dialog 为全屏
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

//            // 设置动画（可选）
//            dialog.window?.setWindowAnimations(R.style.DialogAnimation)
        }
    }

    companion object {
        private var isShown: Boolean = false
        fun newInstance(): DialogBibleGuide? {
            if (!isShown) {
                isShown = true
                if (!MainApp.getContext().getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .getBoolean("has_shown_guide_bible", false)
                ) {
                    val fragment = DialogBibleGuide()
                    return fragment
                }
            }
            return null
        }
    }
}