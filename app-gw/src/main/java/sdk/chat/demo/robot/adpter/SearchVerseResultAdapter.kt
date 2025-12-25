package sdk.chat.demo.robot.adpter

import android.content.Context
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.BibleSearchResult
import sdk.chat.demo.robot.api.model.FavoriteList

class SearchVerseResultAdapter(
    private val onItemClick: (BibleSearchResult) -> Unit,
    private val listData: List<BibleSearchResult>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//    private val listData: MutableList<BibleSearchResult> =
//        ArrayList<BibleSearchResult>()

//    companion object {
//        private const val TYPE_ITEM = 1
//        private const val TYPE_FOOTER = 2
//    }
//
//    var isLoading = false
//        set(value) {
//            if (field != value) {
//                field = value
//                notifyItemChanged(itemCount - 1)
//            }
//        }

//    override fun getItemViewType(position: Int): Int {
////        if (position == listData.size) {
//////            if (position == listData.size && mOnLoadMoreListener.isAllScreen) {
////            return TYPE_FOOTER
////        }
////        return TYPE_ITEM
//        if (position == listData.size) {
//            return TYPE_FOOTER
//        }
//        return TYPE_ITEM
//    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_verse_result, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        try {
            when (holder) {
                is MyViewHolder -> {
                    if (position < listData.size) {
                        val item = listData[position]
                        holder.tvContent.text = Html.fromHtml(item.content, Html.FROM_HTML_MODE_LEGACY)
                        holder.tvRefer.text = item.reference
                        holder.itemView.setOnClickListener { onItemClick(item) }
                        holder.tvContent.setOnClickListener { onItemClick(item) }

                    } else {
                        holder.tvContent.text = "" // 处理异常情况
                    }

                }
            }
        } catch (e: Exception) {
            Log.e("listFavorite1", "Binding error at pos $position", e)
        }
//        if (getItemViewType(position) === TYPE_FOOTER) {
//        } else {
//            val viewHolder = holder as MyViewHolder
//            viewHolder.textView.setText("第" + position + "行")
//        }
    }

    override fun getItemCount(): Int = listData.size

//    fun clear() {
//        listData.clear()
//        notifyDataSetChanged()
//    }
//
//    fun addItems(data: ArrayList<BibleSearchResult>) {
////        if (Looper.myLooper() != Looper.getMainLooper()) {
////            Handler(Looper.getMainLooper()).post { updateData(newData) }
////            return
////        }
//        listData.addAll(data)
//        notifyDataSetChanged()
////        notifyItemRangeInserted(startPos, data.size)
//    }


    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContent: TextView = itemView.findViewById<TextView?>(R.id.tvContent)
        val tvRefer: TextView = itemView.findViewById<TextView?>(R.id.tvRefer)
    }
}