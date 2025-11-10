package sdk.chat.demo.robot.ui

import android.net.Uri
import android.content.Intent
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Toast
import sdk.chat.demo.robot.api.model.Song

class SongsContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var onSongClickListener: OnSongClickListener? = null
    private var songs: MutableList<Song> = mutableListOf()
    open var isMultiSelectMode: Boolean = false

    init {
        orientation = VERTICAL
    }

    fun setSongs(songs: List<Song>,isMultiSelectMode:Boolean) {
        this.isMultiSelectMode = isMultiSelectMode
        clearSongs()
        this.songs.clear()
        this.songs.addAll(songs)
        addSongs(songs)
    }

    fun addSong(song: Song) {
        if(!isMultiSelectMode){
            song.isSelected = false
        }
        val songItemView = SongItemView(context).apply {
            setSong(song,isMultiSelectMode)

//            // 设置点击事件
//            onListenLinkClick = {
//                onSongClickListener?.onListenClick(song)
//            }
//
//            onSheetMusicClick = {
//                onSongClickListener?.onSheetMusicClick(song)
//            }
//
//            // 整个歌曲项点击
//            setOnClickListener {
//                onSongClickListener?.onSongClick(song)
//            }
        }

//        val songItemView = SongItemView(context)

        addView(songItemView)
        songs.add(song)
    }

    fun addSongs(songs: List<Song>) {
        songs.forEach { song ->
            addSong(song)
        }
    }

    fun clearSongs() {
        removeAllViews()
        songs.clear()
    }

    fun updateSong(songId: String, updatedSong: Song) {
        val index = songs.indexOfFirst { it.id == songId }
        if (index != -1) {
            songs[index] = updatedSong
            // 更新对应的视图
            if (index < childCount) {
                val child = getChildAt(index) as? SongItemView
                child?.setSong(updatedSong,isMultiSelectMode)
            }
        }
    }

    fun removeSong(songId: String) {
        val index = songs.indexOfFirst { it.id == songId }
        if (index != -1) {
            songs.removeAt(index)
            if (index < childCount) {
                removeViewAt(index)
            }
        }
    }

    fun setOnSongClickListener(listener: OnSongClickListener) {
        this.onSongClickListener = listener
    }

    fun getSongs(): List<Song> = songs.toList()

    interface OnSongClickListener {
        fun onListenClick(song: Song)
        fun onSheetMusicClick(song: Song)
        fun onSongClick(song: Song)
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }
}