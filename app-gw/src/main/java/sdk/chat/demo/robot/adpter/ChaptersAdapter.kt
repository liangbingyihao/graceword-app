package sdk.chat.demo.robot.adpter

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import sdk.chat.demo.pre.R
import androidx.recyclerview.widget.RecyclerView

class ChaptersAdapter(
    private val chapters: List<Int>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ChaptersAdapter.ViewHolder>() {
//
    private var selectedPosition = -1
    private var lastSelectedPosition = -1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textChapter: TextView = itemView.findViewById(R.id.text_chapter)
//        val chapterContainer: View = itemView.findViewById(R.id.chapter_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.textChapter.text = chapter.toString()
        val colorSelected = ContextCompat.getColor(holder.textChapter.context, R.color.item_text_selected)
        val colorGray = ContextCompat.getColor(holder.textChapter.context, R.color.item_text_normal)


        // 设置选中状态
        if (position == selectedPosition) {
            // 选中状态 - 红色背景，白色文字
            holder.textChapter.setTextColor(colorSelected)
            holder.textChapter.setTypeface(null, Typeface.BOLD)
        } else {
            // 未选中状态 - 灰色边框，黑色文字
            holder.textChapter.setTextColor(colorGray)
            holder.textChapter.setTypeface(null, Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener {
            setSelectedPosition(position)
            onItemClick(chapter)
        }
    }

    override fun getItemCount(): Int = chapters.size

    fun setSelectedPosition(position: Int) {
        lastSelectedPosition = selectedPosition
        selectedPosition = position

        // 优化更新，只更新变化的项目
        if (lastSelectedPosition != -1) {
            notifyItemChanged(lastSelectedPosition)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedChapter(): Int? {
        return if (selectedPosition != -1) chapters[selectedPosition] else null
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = -1
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
    }
}