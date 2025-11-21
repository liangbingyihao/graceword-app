package sdk.chat.demo.robot.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.WebViewActivity
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.api.model.Song
import sdk.chat.demo.robot.handlers.LogUploader
import java.lang.ref.WeakReference
import java.util.List

class SongItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var tvSongTitle: TextView
    private lateinit var tvListenLink: TextView
    private lateinit var tvSheetMusicLink: TextView
    private lateinit var tvPPTLink: TextView
    private lateinit var tvAlbum: TextView
    private lateinit var tvLyrics: TextView
    private lateinit var tvExpandLyrics: TextView
    private lateinit var tvCopyright: TextView
    private lateinit var vDivider1: View
    private lateinit var vDivider2: View
    private lateinit var cbSong: CheckBox

    private var isLyricsExpanded = false
    private val textPaint = Paint()
    private var song: WeakReference<Song>? = null
    private var isMultiSelectMode: Boolean = false

    init {
        initView()
        initTextPaint()
    }

    private fun initTextPaint() {
        textPaint.textSize = tvLyrics.textSize
        textPaint.typeface = tvLyrics.typeface
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.item_song_card, this, true)

        tvSongTitle = findViewById(R.id.tvSongTitle)
        tvListenLink = findViewById(R.id.tvListenLink)
        tvSheetMusicLink = findViewById(R.id.tvSheetMusicLink)
        tvPPTLink = findViewById(R.id.tvPPTLink)
        tvAlbum = findViewById(R.id.tvAlbum)
        tvLyrics = findViewById(R.id.tvLyrics)
        tvExpandLyrics = findViewById(R.id.tvExpandLyrics)
        tvCopyright = findViewById(R.id.tvCopyright)
        vDivider1 = findViewById(R.id.vDivider1)
        vDivider2 = findViewById(R.id.vDivider2)
        cbSong = findViewById(R.id.cb_ai_song)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        tvListenLink.setOnClickListener {
//            onListenLinkClick?.invoke()
            openUrlInBrowser(this.song?.get()?.listenUrl)
            LogUploader.reportEvent(
                "mod_msg_interact", listOf<KeyValuePair?>(
                    KeyValuePair("interact_action", "60")
                )
            )
        }

        tvSheetMusicLink.setOnClickListener {
//            onSheetMusicClick?.invoke()
            openUrlInBrowser(this.song?.get()?.sheetMusicUrl)
            LogUploader.reportEvent(
                "mod_msg_interact", listOf<KeyValuePair?>(
                    KeyValuePair("interact_action", "61")
                )
            )
        }

        tvPPTLink.setOnClickListener {
//            onSheetMusicClick?.invoke()
            openUrlInBrowser(this.song?.get()?.pptUrl)
            LogUploader.reportEvent(
                "mod_msg_interact", listOf<KeyValuePair?>(
                    KeyValuePair("interact_action", "62")
                )
            )
        }

        tvExpandLyrics.setOnClickListener {
            toggleLyricsExpansion()
        }

        setOnClickListener {

            if (isMultiSelectMode) {
                cbSong.isChecked = !cbSong.isChecked
            }
        }

        cbSong.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isMultiSelectMode) {
                Log.e("cbSong", this.song?.get()?.title + "," + isChecked)
                this.song?.get()?.isSelected = isChecked;
            }
        }
    }

    fun setSong(song: Song, isMultiSelectMode: Boolean) {
        this.song = WeakReference(song)
        this.isMultiSelectMode = isMultiSelectMode
        tvSongTitle.text = song.title
        tvAlbum.text =
            context.getString(R.string.album, song.composer, song.lyricist, song.album, song.artist)
        tvLyrics.text = song.lyrics
        tvCopyright.text = song.copyright

        if (isMultiSelectMode) {
            cbSong.visibility = VISIBLE
        } else {
            cbSong.visibility = GONE
            song.isSelected = false
        }
        Log.e("cbSong", this.song?.get()?.title + ",isMultiSelectMode:" + isMultiSelectMode)

        var showDivider1 = VISIBLE
        var showDivider2 = VISIBLE

        if (!song.listenUrl.isNullOrBlank() && song.listenUrl.startsWith("http")) {
            tvListenLink.visibility = VISIBLE
        } else {
            tvListenLink.visibility = GONE
            showDivider1 = GONE
        }

        if (!song.sheetMusicUrl.isNullOrBlank() && song.sheetMusicUrl.startsWith("http")) {
            tvSheetMusicLink.visibility = VISIBLE
        } else {
            tvSheetMusicLink.visibility = GONE
            showDivider2 = GONE
        }

        if (!song.pptUrl.isNullOrBlank() && song.pptUrl.startsWith("http") && song.pptUrl.endsWith(
                "ppt",
                true
            )
        ) {
            tvPPTLink.visibility = VISIBLE
        } else {
            tvPPTLink.visibility = GONE
            if (showDivider2 == VISIBLE) {
                showDivider2 = GONE
            } else if (showDivider1 == VISIBLE) {
                showDivider1 = GONE
            }
        }
        vDivider1.visibility = showDivider1
        vDivider2.visibility = showDivider2


        // 预计算歌词行数，避免布局循环
        preCalculateLyricsLines(song.lyrics)
    }

    private fun preCalculateLyricsLines(lyrics: String?) {
        if (lyrics.isNullOrBlank()) {
            return
        }
        // 获取TextView的宽度（减去padding）
        val availableWidth = width - paddingLeft - paddingRight
        if (availableWidth <= 0) {
            // 如果宽度未知，延迟计算
            post { preCalculateLyricsLines(lyrics) }
            return
        }

        // 计算文本行数
        val lineCount = calculateLineCount(lyrics, availableWidth)
        val shouldShowExpand = lineCount > 7

        if (shouldShowExpand) {
            tvExpandLyrics.visibility = View.VISIBLE
            if (!isLyricsExpanded) {
                tvLyrics.maxLines = 7
                tvExpandLyrics.text = context.getString(R.string.unfold)
            } else {
                tvLyrics.maxLines = Integer.MAX_VALUE
                tvExpandLyrics.text = context.getString(R.string.fold)
            }
        } else {
            tvExpandLyrics.visibility = View.GONE
            tvLyrics.maxLines = Integer.MAX_VALUE
        }
    }

    private fun calculateLineCount(text: String, availableWidth: Int): Int {
        if (availableWidth <= 0) return 1

        var lineCount = 1
        var currentLineWidth = 0f

        text.forEach { char ->
            val charWidth = textPaint.measureText(char.toString())
            if (currentLineWidth + charWidth > availableWidth) {
                lineCount++
                currentLineWidth = charWidth
            } else {
                currentLineWidth += charWidth
            }
        }

        return lineCount
    }

    private fun toggleLyricsExpansion() {
        isLyricsExpanded = !isLyricsExpanded
        updateLyricsExpansionState()
    }

    private fun updateLyricsExpansionState() {
        if (isLyricsExpanded) {
            tvLyrics.maxLines = Integer.MAX_VALUE
            tvExpandLyrics.text = context.getString(R.string.fold)
//            LogUploader.reportEvent(
//                "mod_msg_interact", listOf<KeyValuePair?>(
//                    KeyValuePair("interact_action", "63")
//                )
//            )
        } else {
            tvLyrics.maxLines = 7
            tvExpandLyrics.text = context.getString(R.string.unfold)
//            LogUploader.reportEvent(
//                "mod_msg_interact", listOf<KeyValuePair?>(
//                    KeyValuePair("interact_action", "64")
//                )
//            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 尺寸变化时重新计算
        if (w != oldw && tvLyrics.text.isNotEmpty()) {
            preCalculateLyricsLines(tvLyrics.text.toString())
        }
    }

    // 点击事件回调
    var onListenLinkClick: (() -> Unit)? = null
    var onSheetMusicClick: (() -> Unit)? = null
    var onLyricsExpandClick: (() -> Unit)? = null

    private fun openUrlInBrowser(url: String?) {
        if (url == null || url.isEmpty()) {
            return
        }
        //FIXME
        if (true||url.endsWith("ppt", true) || url.endsWith("pdf", true)) {
            try {
                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )
                // 确保只有浏览器能处理此Intent（排除其他可能的应用）
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                // 禁止弹出应用选择对话框（直接使用默认浏览器）
                intent.setPackage(null)
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
            }
        }
        WebViewActivity.launchWithUrl(context, url, this.song?.get()?.title)
//        return
//        try {
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//
//            // 检查是否有浏览器可以处理该 Intent
//            if (intent.resolveActivity(context.packageManager) != null) {
//                context.startActivity(intent)
//            } else {
//                ToastHelper.show(context, "未找到可用的浏览器")
//            }
//        } catch (e: Exception) {
//            Log.e("SongItemView", "打开链接失败: ${e.message}", e)
//            ToastHelper.show(context, "打开链接失败")
//        }
    }
}