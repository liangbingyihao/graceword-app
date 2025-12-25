package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.Verse
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK

class VerseAdapter(
    private val verses: List<Verse>,
//    private val onVerseSelected: (Int, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<VerseAdapter.VerseViewHolder>() {

    // 记录选中的经文位置
    private val selectedPositions = mutableSetOf<Int>()

    private var _isMultiSelectMode = false
    val isMultiSelectMode: Boolean
        get() = _isMultiSelectMode

    fun setMultiSelectMode(enabled: Boolean) {
        if (_isMultiSelectMode != enabled) {
            _isMultiSelectMode = enabled
            if (!_isMultiSelectMode) {
                selectedPositions.clear()
            }
            notifyDataSetChanged()
        }
    }

    inner class VerseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val verseNumber: TextView = itemView.findViewById(R.id.verse_number)
        val verseText: TextView = itemView.findViewById(R.id.verse_text)
        val cbVerse: CheckBox? = itemView.findViewById(R.id.cb_verse)

        // 将监听器定义为类变量
        private val onClickListener = View.OnClickListener { view ->
            val position = getSafeAdapterPosition()
            if (position != RecyclerView.NO_POSITION) {
                toggleSelection(position)
            }
        }

        private val onLongClickListener = View.OnLongClickListener {
            val position = getSafeAdapterPosition()
            if (position != RecyclerView.NO_POSITION) {
                selectedPositions.add(position)
                setMultiSelectMode(true)
                ChatSDK.events().source()
                    .accept(NetworkEvent(EventType.ShowVerseMenus))
                return@OnLongClickListener true
            }
            false
        }

        private fun getSafeAdapterPosition(): Int {
            // 优先使用 bindingAdapterPosition，它是 RecyclerView 推荐的获取位置的方法
            val position = bindingAdapterPosition
            // 如果 bindingAdapterPosition 无效，回退到 adapterPosition
            return if (position != RecyclerView.NO_POSITION) position else adapterPosition
        }

        init {
//            // 设置item点击事件
//            itemView.setOnClickListener {
////                val position = adapterPosition
//                val position = bindingAdapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    toggleSelection(position)
//                }
//            }
//            itemView.setOnLongClickListener {
//                val position = bindingAdapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    selectedPositions.add(position)
//                }
//                setMultiSelectMode(true)
//
//                ChatSDK.events().source()
//                    .accept(NetworkEvent(EventType.ShowVerseMenus))
//                true
//            }
            itemView.setOnClickListener(onClickListener)
            itemView.setOnLongClickListener(onLongClickListener)
            cbVerse?.setOnClickListener(onClickListener)
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
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.selected_verse_background
                )
            )
            holder.cbVerse?.isChecked = true
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.default_verse_background
                )
            )
            holder.cbVerse?.isChecked = false
        }

        if (isMultiSelectMode) {
            holder.cbVerse?.visibility = View.VISIBLE
        } else {
            holder.cbVerse?.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return verses.size
    }

    // 切换选中状态
    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
//            onVerseSelected(position, false)
        } else {
            selectedPositions.add(position)
//            onVerseSelected(position, true)
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

//    // 清除所有选中状态
//    fun clearSelections() {
//        val positions = selectedPositions.toList()
//        selectedPositions.clear()
//        positions.forEach { notifyItemChanged(it) }
//    }

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
