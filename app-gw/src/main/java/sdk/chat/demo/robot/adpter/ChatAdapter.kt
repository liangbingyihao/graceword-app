package sdk.chat.demo.robot.adpter

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.size
import androidx.recyclerview.widget.RecyclerView
import com.stfalcon.chatkit.commons.models.IMessage
import org.pmw.tinylog.Logger
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.data.AIExplore
import sdk.chat.demo.robot.api.model.GWConfigs.WelcomeSurvey
import sdk.chat.demo.robot.holder.ChatImageViewHolder
import sdk.chat.demo.robot.holder.ChatTextViewHolder
import sdk.chat.demo.robot.holder.ExploreHolder
import sdk.chat.demo.robot.holder.ExploreViewHolder
import sdk.chat.demo.robot.holder.ImageHolder
import sdk.chat.demo.robot.holder.MessageHolder
import sdk.chat.demo.robot.holder.SongsContainerViewHolder
import sdk.chat.demo.robot.holder.TextHolder
import sdk.chat.demo.robot.holder.TimeHolder
import sdk.chat.demo.robot.holder.WelcomeHolder
import sdk.chat.demo.robot.holder.WelcomeViewHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ChatAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TEXT = 1
        private const val TYPE_IMAGE = 2
        private const val TYPE_TIME = 3
        private const val TYPE_SONG = 4
        private const val TYPE_FOOTER = 5
        private const val TYPE_WELCOME = 6
    }

    private var _isMultiSelectMode = false
    val isMultiSelectMode: Boolean
        get() = _isMultiSelectMode

    fun setMultiSelectMode(enabled: Boolean) {
        if (_isMultiSelectMode != enabled) {
            _isMultiSelectMode = enabled
            if (!_isMultiSelectMode) {
                clearSelections()
            }
            notifyDataSetChanged()
        }
    }

    private val items = mutableListOf<IMessage>()

    //    private var header: Any? = null
    private var footer: Any? = null
    private val viewClickListenersArray =
        SparseArray<OnMessageViewClickListener>()

    interface OnMessageViewClickListener {
        /**
         * Fires when message view is clicked.
         *
         * @param message clicked message.
         */
        fun onMessageViewClick(view: View?, message: IMessage?)
    }

//    init {
//        if (items.isEmpty()) {
//            items.add(ExploreHolder())
//        }
//    }

    //    private val exploreHolder = ExploreHolder()
    var aiExplore: AIExplore? = null
        set(value) {
            field = value
            Log.e("AIExplore", "set AIExplore:" + aiExplore?.message?.id)
            Handler(Looper.getMainLooper()).postDelayed(
                { notifyItemChanged(0); },
                2
            )
        }

    var header = false
        get() = field
        set(value) {
            if (field != value) {
                field = value
//                Handler(Looper.getMainLooper()).postDelayed({ notifyItemChanged(itemCount-1); },2)
            }
        }

    fun registerViewClickListener(
        viewId: Int,
        onMessageViewClickListener: OnMessageViewClickListener
    ) {
        viewClickListenersArray.append(viewId, onMessageViewClickListener)
    }

    fun bindListeners(holder: RecyclerView.ViewHolder, item: IMessage?) {
        for (i in 0..<viewClickListenersArray.size) {
            val key: Int = viewClickListenersArray.keyAt(i)
            val view: View? = holder.itemView.findViewById<View?>(key)
            view?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    viewClickListenersArray.get(key).onMessageViewClick(view, item)
                }
            })
        }
    }

    fun clear() {

        Log.e("AIExplore", "clear")
        if (items.isNotEmpty()) {
            items.clear()
            notifyDataSetChanged()
        }
    }

    // 清除所有选择
    fun clearSelections() {
        for (item in items) {
            (item as? TextHolder)?.apply {
                isAiSelected = false
                isUserSelected = false
            }
        }
    }


    // 获取选中的项目
    fun getSelectedItems(): MutableList<TextHolder?> {
        val selectedItems: MutableList<TextHolder?> = ArrayList<TextHolder?>()
        for (item in items) {
            (item as? TextHolder)?.takeIf { (it.isAiSelected &&!it.isSong) || it.isUserSelected || (it.isSong&&it.hasSelectedHymns()) }
                ?.run {
                    selectedItems.add(item)
                }
        }
        return selectedItems
    }

    // 获取选中的项目
    fun getCntSelected(): Int {
        var total = 0
        for (item in items) {
            (item as? TextHolder)?.let { holder ->
                if (holder.isAiSelected) total++
                if (holder.isUserSelected) total++
            }
        }
        return total
    }

    // 添加新消息（自动插入到头部）
    fun addNewMessage(item: IMessage, onComplete: (() -> Unit)? = null) {
//        val newList = items.toMutableList().apply { add(1, item) }
//        submitList(newList, onComplete)
        Log.e("AIExplore", "addNewMessage:${item.id}")
        var oldSize = items.size
        val oldHeader = if (oldSize > 0) {
            getItemViewType(oldSize - 1) == TYPE_HEADER
        } else {
            false // 如果 itemCount 为 0，直接返回 false
        }

        if (oldHeader) {
            items[oldSize - 1] = item
        } else {
            items.add(item)
        }

        var msg = (item as? MessageHolder)?.message
        items.add(ExploreHolder(msg))
        notifyItemRangeChanged(oldSize, 2)

//        // 批量通知
//        if (oldHeader) {
//            notifyItemInserted(oldSize)     // 新项插入
//            notifyItemChanged(oldSize - 1)      // 头部内容变化
//        } else {
//            notifyItemRangeInserted(oldSize, 2)  // 两个新项插入
//        }
        onComplete?.invoke()
    }

    /**
     * 添加多条新消息到列表头部（支持批量操作和DiffUtil优化）
     * @param newMessages 要添加的消息集合
     * @param onComplete 操作完成回调（可选，在主线程执行）
     */
    fun addNewMessage(newMessages: List<IMessage>, onComplete: (() -> Unit)? = null) {
        if (newMessages.isEmpty()) {
            onComplete?.invoke()
            return
        }
//        if (itemCount == 0) {
//            var msgHolder = newMessages[0]
//            var message = when (msgHolder) {
//                is TextHolder -> msgHolder.message
//                is ImageHolder -> msgHolder.message
//                else -> null
//            }
//            if(message!=null){
//                items.add(ExploreHolder(message))
//            }
//        }
        Log.e("AIExplore", "addNewMessage List")

        var msg = (newMessages[newMessages.lastIndex] as? MessageHolder)?.message
        var oldSize = items.size
        val oldHeader = if (oldSize > 0) {
            getItemViewType(oldSize - 1) == TYPE_HEADER
        } else {
            false // 如果 itemCount 为 0，直接返回 false
        }

        if (oldHeader) {
            items.addAll(oldSize - 1, newMessages)
            items[oldSize + newMessages.size - 1] = ExploreHolder(msg)
        } else {
            items.addAll(newMessages)
            items.add(ExploreHolder(msg))
        }
        notifyItemRangeChanged(oldSize, newMessages.size)

//        items.addAll(1, newMessages)
//        notifyDataSetChanged()

//        // 批量通知
//        if (oldHeader) {
//            notifyItemChanged(0)      // 头部内容变化
//            notifyItemRangeInserted(1, newMessages.size)     // 新项插入
//        } else {
//            notifyItemRangeInserted(0, newMessages.size + 1)  // 两个新项插入
//        }
        onComplete?.invoke()


    }

    // 批量添加历史消息
    fun addHistoryMessages(newItems: List<IMessage>, onComplete: (() -> Unit)? = null) {
        Log.e("AIExplore", "addHistoryMessages:${newItems.size}")
//        val newList = items.toMutableList().apply { addAll(newItems) }
//        submitList(newList, onComplete)
        var s = itemCount
//        if (itemCount == 0 && newItems.isNotEmpty()) {
//            (newItems[0] as? MessageHolder)?.message?.let { items.add(ExploreHolder(it)) }
//        }
        items.addAll(0, newItems)
        notifyItemRangeInserted(0, newItems.size)
        if (s == 0 && newItems.isNotEmpty()) {
            (newItems[newItems.lastIndex] as? MessageHolder)?.message?.let {
                items.add(
                    ExploreHolder(
                        it
                    )
                )
            }
            notifyItemInserted(itemCount)
        }
        onComplete?.invoke()
    }


    fun delMessage(item: IMessage, onComplete: (() -> Unit)? = null) {
        // Create new list with items removed
        val deletePos = items.indexOfFirst { it.id == item.id }

        if (deletePos == -1) {
//            Log.e("delmsg", "message.id:${message.id},del:-1,size:${items.size}")
            onComplete?.invoke()
            return
        }

        Log.e("AIExplore", "delMessage:${item.id},deletePos:${deletePos}")
//        var newExploreHolder: ExploreHolder? = null
//        if (itemCount > deletePos) {
//            var msg = (items[deletePos + 1] as? MessageHolder)?.message
//            newExploreHolder = ExploreHolder(msg)
//        }

//        var msg = (item as? MessageHolder)?.message
        items.removeAt(deletePos)
        if (deletePos == 1) {
            val oldExploreMsg = (items[0] as? ExploreHolder)?.message
            if (oldExploreMsg != null && oldExploreMsg.entityID == item.id) {
                Log.e("AIExplore", "delMessage:${item.id},deletePos:${deletePos}, and del explore")
                items.removeAt(0)
                notifyItemRangeRemoved(0, 2)
                onComplete?.invoke()
                return
            }
        }
        notifyItemRemoved(deletePos)
        onComplete?.invoke()

    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ExploreHolder -> TYPE_HEADER
            is TextHolder -> {
                if (item.isSong) {
                    TYPE_SONG
                } else if (WelcomeHolder.isWelcomeMsg(item.message)) {
                    TYPE_WELCOME
                } else {
                    TYPE_TEXT
                }
            }

            is ImageHolder -> TYPE_IMAGE
            is TimeHolder -> TYPE_TIME
            is WelcomeHolder -> TYPE_WELCOME
            else -> throw IllegalStateException("Unknown message type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (TYPE_HEADER == viewType) {
            Logger.warn("onLoadCreateView:" + viewType)
        }
        return when (viewType) {
            TYPE_HEADER -> ExploreViewHolder(inflateView(R.layout.item_feed_header, parent))
            TYPE_TEXT -> ChatTextViewHolder<TextHolder>(
                inflateView(
                    R.layout.item_feed_text,
                    parent
                )
            )

            TYPE_SONG -> SongsContainerViewHolder<TextHolder>(
                inflateView(
                    R.layout.item_songs_container,
                    parent
                )
            )

            TYPE_IMAGE -> ChatImageViewHolder<ImageHolder>(
                inflateView(
                    R.layout.item_feed_daily_gw,
                    parent
                )
            )

            TYPE_WELCOME -> WelcomeViewHolder(
                inflateView(
                    R.layout.item_feed_welcome,
                    parent
                )
            )

            TYPE_TIME -> TimeViewHolder(inflateView(R.layout.item_date_header, parent))
            TYPE_FOOTER -> FooterViewHolder(inflateView(R.layout.item_list_footer, parent))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

//        Log.e("AIExplore", "onBindViewHolder:" + position+","+holder)
        when (holder) {
            is ChatTextViewHolder<*> -> {
                val item = getItem(position)
                try {
                    @Suppress("UNCHECKED_CAST")
                    (holder as ChatTextViewHolder<TextHolder>).onBind(
                        item as TextHolder,
                        isMultiSelectMode,
                        position
                    )
                    bindListeners(holder, item)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }

            is SongsContainerViewHolder<*> -> {
                val item = getItem(position)
                try {
                    @Suppress("UNCHECKED_CAST")
                    (holder as SongsContainerViewHolder<TextHolder>).onBind(
                        item as TextHolder,
                        isMultiSelectMode,
                        position
                    )
                    bindListeners(holder, item)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }

            is ChatImageViewHolder<*> -> {
                val item = getItem(position)
                try {
                    @Suppress("UNCHECKED_CAST")
                    (holder as ChatImageViewHolder<ImageHolder>).onBind(
                        item as ImageHolder,
                        isMultiSelectMode
                    )
                    bindListeners(holder, item)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }

            is TimeViewHolder -> holder.bind(getItem(position) as TimeHolder)
            is ExploreViewHolder -> {
                val item = getItem(position)
                holder.bind(header, item as ExploreHolder)
            }

            is WelcomeViewHolder -> {
                val item = getItem(position)
                holder.bind(item as WelcomeHolder)
            }

            is FooterViewHolder -> holder.bind(footer)
        }
    }

    override fun getItemCount(): Int = items.size

    /* 内部工具方法 */
    private fun inflateView(layoutId: Int, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    private fun getItem(position: Int): IMessage {
        return items[position]
    }


    inner class TimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(divider: TimeHolder) {
            itemView.findViewById<TextView>(R.id.messageText).text =
                SimpleDateFormat("MM月dd日", Locale.CHINA)
                    .format(divider.createdAt)
        }
    }

    inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Any?) {
            // 根据footer类型处理
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= items.size) {
            return RecyclerView.NO_ID
        }
        val item = getItem(position)
        return when {
            else -> when (item) {
                is TextHolder -> item.message.id
                is ImageHolder -> item.message.id
                else -> RecyclerView.NO_ID
            }
        }
    }
}