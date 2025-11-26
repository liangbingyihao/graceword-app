package sdk.chat.demo.robot.adpter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.BibleBook

class BibleBookAdapter(
    private var books: List<BibleBook>,
    private val onItemClick: (BibleBook) -> Unit
) : RecyclerView.Adapter<BibleBookAdapter.ViewHolder>() {

    private var selectedPosition = -1
    private var lastSelectedPosition = -1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textBookName: TextView = itemView.findViewById(R.id.text_book_name)
//        val selectionIndicator: View = itemView.findViewById(R.id.selection_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bible_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        holder.textBookName.text = book.name

        val colorSelected = ContextCompat.getColor(holder.textBookName.context, R.color.item_text_selected)
        val colorGray = ContextCompat.getColor(holder.textBookName.context, R.color.item_text_normal)

        // 设置选中状态
        if (position == selectedPosition) {
            // 选中状态样式
            holder.textBookName.setTextColor(colorSelected)
            holder.textBookName.textSize = 18f
            holder.textBookName.setTypeface(null, colorSelected)
//            holder.selectionIndicator.visibility = View.VISIBLE
        } else {
            // 未选中状态样式
            holder.textBookName.setTextColor(colorGray)
            holder.textBookName.textSize = 16f
            holder.textBookName.setTypeface(null, Typeface.NORMAL)
//            holder.selectionIndicator.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            setSelectedPosition(position)
            onItemClick(book)
        }
    }

    override fun getItemCount(): Int = books.size

    fun updateBooks(newBooks: List<BibleBook>) {
        books = newBooks
        selectedPosition = -1 // 重置选中状态
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        lastSelectedPosition = selectedPosition
        selectedPosition = position

        // 只更新变化的项目，提高性能
        if (lastSelectedPosition != -1) {
            notifyItemChanged(lastSelectedPosition)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = -1
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
    }
}