package sdk.chat.demo.robot.activities

import android.util.Log;
import android.content.Context
import android.content.Intent
import android.os.Bundle
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.ui.KeyboardAwareFrameLayout
import sdk.chat.demo.robot.ui.KeyboardAwareFrameLayout.HeightUpdater

class NoteEditActivity : BaseActivity() {
    companion object {
        private const val ARG_BOOK = "book_id"
        private const val ARG_REFERENCE = "reference"
        private const val ARG_FULLSCREEN = "fullscreen"
        private const val ARG_CHAPTER_NUMBER = "chapter_number"
        private const val TAG = "NoteEditActivity"

        // 提供静态启动方法（推荐）
        fun start(
            context: Context?,
            reference: String = "",
            fullscreen: Boolean = false,
            bookId: Int = 0,
            chapterNumber: Int = 0,
            newTask: Boolean = false,
        ) {
            val intent = Intent(context, NoteEditActivity::class.java).apply {
                putExtra(ARG_BOOK, bookId)
                putExtra(ARG_REFERENCE, reference)
                putExtra(ARG_FULLSCREEN, fullscreen)
                putExtra(ARG_CHAPTER_NUMBER, chapterNumber)
            }
            if(newTask){
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context?.startActivity(intent)
        }
    }

    override fun getLayout(): Int {
        return 0;
    }

//    private lateinit var root: KeyboardAwareFrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note1)

//        // 设置Toolbar
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)
        var reference = intent.getStringExtra(ARG_REFERENCE).toString()
        if (!reference.isEmpty()) {
            reference = reference.removeSurrounding("(", ")")
        }


        var bookId = intent.getIntExtra(ARG_BOOK, 0)
        var chapterNumber = intent.getIntExtra(ARG_CHAPTER_NUMBER, 0)
//        setupKeyboardListeners()

    }
//    private fun setupKeyboardListeners() {
//        root = findViewById<KeyboardAwareFrameLayout?>(R.id.root)
//        root.keyboardShownListeners.add(Runnable {
//
//            // We want the bottom margin to be just the height of the input + reply view
////            setChatViewBottomMargin(bottomMargin());
//            updateEditBottomMargin()
//        })
//
//        root.keyboardHiddenListeners.add(Runnable {
//
//            updateEditBottomMargin()
//        })
//
//        root.heightUpdater =
//            HeightUpdater { height: Int -> Log.e(TAG,"heightUpdater${root.keyboardHeight},${height}") }
//    }
//
//    private fun updateEditBottomMargin(){
//        Log.e(TAG,"updateEditBottomMargin${root.keyboardHeight}")
//    }
}