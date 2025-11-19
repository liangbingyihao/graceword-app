package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.Verse
import androidx.recyclerview.widget.RecyclerView

class VerseAdapter(
    private val verses: List<Verse>,
    private val onVerseSelected: (Int, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<VerseAdapter.VerseViewHolder>() {

    // 记录选中的经文位置
    private val selectedPositions = mutableSetOf<Int>()

    inner class VerseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val verseNumber: TextView = itemView.findViewById(R.id.verse_number)
        val verseText: TextView = itemView.findViewById(R.id.verse_text)

        init {
            // 设置item点击事件
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleSelection(position)
                }
            }
        }
    }

    init {
        // 初始化：将 referenced=true 的经文索引加入选中集合
        verses.forEachIndexed { index, verse ->
            if (verse.referenced == true) {
                selectedPositions.add(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verse, parent, false)
        return VerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerseViewHolder, position: Int) {
        val verse = verses[position]
        holder.verseNumber.text = verse.verseNumber.toString()
        holder.verseText.text = verse.text

        // 根据选中状态设置背景
        if (selectedPositions.contains(position)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context,R.color.selected_verse_background))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context,R.color.default_verse_background))
        }
    }

    override fun getItemCount(): Int {
        return verses.size
    }

    // 切换选中状态
    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
            onVerseSelected(position, false)
        } else {
            selectedPositions.add(position)
            onVerseSelected(position, true)
        }
        notifyItemChanged(position)
    }

    // 设置选中状态
    fun setSelected(position: Int, selected: Boolean) {
        if (selected) {
            selectedPositions.add(position)
        } else {
            selectedPositions.remove(position)
        }
        notifyItemChanged(position)
    }

    // 清除所有选中状态
    fun clearSelections() {
        val positions = selectedPositions.toList()
        selectedPositions.clear()
        positions.forEach { notifyItemChanged(it) }
    }

    // 获取所有选中的位置
    fun getSelectedPositions(): Set<Int> {
        return selectedPositions.toSet()
    }

    // 获取所有选中的经文
    fun getSelectedVerses(): List<Verse> {
        return selectedPositions.mapNotNull { position ->
            verses.getOrNull(position)
        }
    }
}
