package sdk.chat.demo.robot.holder

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.functions.Predicate
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.BillingActivity
import sdk.chat.demo.robot.adpter.data.AIExplore
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.GWMsgHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.guru.common.DisposableMap
import sdk.guru.common.RX
import java.util.List

open class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var contentLoadingProgressBar: ContentLoadingProgressBar =
        itemView.findViewById<ContentLoadingProgressBar?>(
            R.id.pb_progress
        )
    val exploreView: Map<String, TextView> = mapOf(
        "explore0" to itemView.findViewById<TextView>(R.id.explore1),
        "explore1" to itemView.findViewById<TextView>(R.id.explore2),
        "explore2" to itemView.findViewById<TextView>(R.id.explore3)
    )
    val placeHolderView = itemView.findViewById<View>(R.id.placeholder)
    val vipText = itemView.findViewById<View>(R.id.vip_text)
    val vipInvite = itemView.findViewById<View>(R.id.button_start_vip)
    open val dm = DisposableMap()
    var loading: Boolean = false

    fun bind(loading: Boolean, aiExplore: ExploreHolder) {
        bindListeners(aiExplore)
        // 根据header类型处理
        this.loading = loading
        contentLoadingProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        bindExplore(aiExplore)
        var action = aiExplore.message.integerForKey("action")
        if (!BillingManager.getInstance().hasSubscriptions()
            && action == AIExplore.ExploreItem.action_guest_talk
        ) {
            vipText.visibility = View.VISIBLE
            vipInvite.visibility = View.VISIBLE
        } else {
            vipText.visibility = View.GONE
            vipInvite.visibility = View.GONE
        }
    }

    open fun bindExplore(t: ExploreHolder) {
        bindListeners(t)
        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler

        var i = 0
        var aiExplore: AIExplore? = t.aiExplore
        val aiFeedback = GWMsgHandler.getAiFeedback(aiExplore?.message)
        var status = t.message?.messageStatus
        Log.e("AIExplore", "bindExplore:" + t.message?.id + ",status:" + status+",size:"+ aiExplore?.itemList?.size)
        if (status != MessageSendStatus.Sent) {
            placeHolderView?.visibility = View.VISIBLE
        } else {
            placeHolderView?.visibility = View.GONE
        }
//        Log.d("sending","threadHandler.isSendingMsg:${threadHandler.pendingMsgId()},aiExplore:${aiExplore?.message?.id},${aiExplore?.itemList?.size}");
        while (i < 3) {
            var v: TextView = exploreView.getValue("explore$i")
            if (aiExplore != null && i < aiExplore.itemList.size) {
//                Log.d("sending", "bindExplore:visible $i");
                var data = aiExplore.itemList[i]
                v.visibility = View.VISIBLE
                v.text = data.text
                if (data.action == AIExplore.ExploreItem.action_bible_pic) {
                    var bible = aiFeedback?.feedback?.bible ?: ""
                    if (bible.isEmpty()) {
                        v.visibility = View.GONE
                        continue
                    }
//                    bible = aiFeedback?.feedbackText ?:""
                    v.setOnClickListener { view ->
                        // 可以使用view参数
                        if (aiFeedback != null && !bible.isEmpty()) {
                            view as TextView
                            threadHandler.sendExploreMessage(
                                view.text.toString().trim(),
                                aiExplore.message,
                                data.action,
                                "${aiFeedback.feedback.tag}|${bible}"
                            ).subscribe();
                        } else {
                            Toast.makeText(v.context, "没有经文...", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else if (data.action == AIExplore.ExploreItem.action_input_prompt) {
                    var event =
                        NetworkEvent.messageInputPrompt(data.text, data.getParamsStr())
                    v.setOnClickListener { view ->
                        ChatSDK.events().source().accept(event)
                    }
                } else if (data.action == AIExplore.ExploreItem.action_input_prompt_welcome) {
                    var event =
                        NetworkEvent.messageInputPrompt(null, data.text)
                    var optionId =(i+1).toString()
                    v.setOnClickListener { view ->
                        ChatSDK.events().source().accept(event)
                        LogUploader.reportEvent(
                            "mod_guide", listOf<KeyValuePair?>(
                                KeyValuePair("guide_action", "20"),
                                KeyValuePair("guide_option_id", optionId)
                            )
                        )
                    }
                } else {
                    v.setOnClickListener { view ->
                        view as TextView // 安全转换
                        threadHandler.sendExploreMessage(
                            view.text.toString().trim(),
                            aiExplore.message,
                            data.action,
                            data.getParamsStr()
                        ).subscribe();
                        LogUploader.reportEvent(
                            "mod_chat", listOf<KeyValuePair?>(
                                KeyValuePair("chat_action", "40")
                            )
                        )
                    }
                }
            } else {
                v.visibility = View.GONE
            }
            ++i
        }
    }

    fun filterById(id: Long?): Predicate<NetworkEvent?> {
//        Log.e("AIExplore", "ExploreViewHolder.MessageUpdated:" + id)
        return Predicate { networkEvent: NetworkEvent? -> networkEvent?.message?.id == id }
    }

    open fun bindListeners(t: ExploreHolder) {
        if (WelcomeHolder.isWelcomeMsg(t.message)) {
            return
        } else {
            dm.dispose()
            dm.add(
                ChatSDK.events().sourceOnSingle()
                    .filter(NetworkEvent.filterType(EventType.MessageUpdated))
                    .filter(filterById(t.message.id))
                    .subscribe {
                        RX.main().scheduleDirect {
                            t.aiExplore = null
                            Log.e("AIExplore", "EventType.MessageUpdated:" + t.message.id)
                            bind(this.loading, t)
                        }
                    })
            vipInvite.setOnClickListener {
                BillingActivity.start(vipInvite.context,"reply_limit")
            }
        }
    }
}