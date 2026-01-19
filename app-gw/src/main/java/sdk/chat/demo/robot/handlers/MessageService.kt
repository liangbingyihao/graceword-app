package sdk.chat.demo.robot.handlers

import android.util.Log
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.dao.Keys
import sdk.chat.core.dao.Message
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.core.types.MessageType
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.api.model.MessageList
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import java.util.Date
import java.util.concurrent.TimeUnit

object MessageService {
    private const val TAG = "MessageService"
    private val gson: Gson = Gson()


    fun loadSessionMessagesAndSaveToLocal(
        sessionId: Long?,
        olderThan: Long?,
        page: Int,
        limit: Int
    ): Completable {
        return GWApiManager.shared().listSessionMessage(sessionId, olderThan, page, limit)
            .flatMapCompletable { messages ->
                Completable.fromAction {
                    saveMessagesToLocal(messages, sessionId)
                    Log.d(TAG, "服务器信息长度:${messages.items.size}")
                    var hasMore = true
                    if (!messages.items.isEmpty() && messages.items.size < limit) {
                        hasMore = false
                        if (sessionId != null) {
                            var thread = ChatSDK.db().fetchThreadWithEntityID(sessionId.toString())
                            if (thread != null) {
                                var lastUpdatedTs =
                                    messages.items[messages.items.size - 1].updatedTs
                                thread.setMetaValue(
                                    Keys.KEY_VERSION,
                                    lastUpdatedTs
                                );
                                Log.d(TAG, "$sessionId set lastUpdatedTs:${lastUpdatedTs}")
                            }
                        }
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .timeout(5, TimeUnit.SECONDS)
            .doOnSubscribe { Log.d(TAG, "开始从服务器加载消息") }
            .doOnComplete { Log.d(TAG, "服务器消息处理完成") }
            .doOnError { error -> Log.e(TAG, "服务器消息处理失败", error) }
            .onErrorComplete()
    }


    fun loadMessagesAndSaveToLocal(olderThan: String?, page: Int, limit: Int): Completable {
        return GWApiManager.shared().listMessage(olderThan, page, limit)
            .flatMapCompletable { messages ->
                Completable.fromAction {
                    Log.d(TAG, "服务器信息长度:${messages.items.size}")
                    saveMessagesToLocal(messages, null)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .timeout(5, TimeUnit.SECONDS)
            .doOnSubscribe { Log.d(TAG, "开始从服务器加载消息") }
            .doOnComplete { Log.d(TAG, "服务器消息处理完成") }
            .doOnError { error -> Log.e(TAG, "服务器消息处理失败", error) }
            .onErrorComplete()
    }

    private fun saveMessagesToLocal(messages: MessageList, sessionId: Long?) {
        try {
            val daoCore = ChatSDK.db().getDaoCore()
            var sender = ChatSDK.currentUser().id
            for (item in messages.items) {
                try {
                    var createdAt: Date = if (item.createdTs != null && item.createdTs > 0) {
                        Date(item.createdTs)
                    } else {
                        Date()
                    }
                    Log.d(
                        TAG,
                        "createdTs: " + item.id + "," + item.createdTs + "," + DateLocalizationUtil.dateStr(
                            createdAt
                        )
                    )

                    var message = ChatSDK.db().fetchMessageWithEntityID(item.id)
                    if (message == null) {
                        message = Message().apply {
                            id = createdAt.time
                            entityID = item.id
                            date = createdAt
                            senderId = sender
                            type = MessageType.Text
                            status = MessageSendStatus.Sent.ordinal
                            threadId = item.sessionId
                        }
                        if (item.status == MessageDetail.STATUS_DELETED) {
                            message.status = MessageSendStatus.Deleted.ordinal
                            Log.d(TAG, "saveMessagesToLocal " + item.id + "," + message.status)
                        }
                        daoCore.createEntity(message)
                        message.text = item.content
                        message.setMetaValue(GWThreadHandler.KEY_AI_FEEDBACK, gson.toJson(item))
                        message.setMetaValue(Keys.KEY_VERSION, item.updatedTs)
                    } else {
                        if (sessionId == null) {
                            message.setMetaValue(GWThreadHandler.KEY_AI_FEEDBACK, gson.toJson(item))
                        }
                        message.setMetaValue(Keys.KEY_VERSION, item.updatedTs)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "convertedMessages失败", e)
                }
            }

            Log.d(TAG, "✅ 成功保存 ${messages.items.size} 条消息到本地数据库")

        } catch (e: Exception) {
            Log.e(TAG, "保存消息到数据库失败", e)
        }
    }
}