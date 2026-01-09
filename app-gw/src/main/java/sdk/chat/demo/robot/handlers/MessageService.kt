package sdk.chat.demo.robot.handlers

import android.util.Log
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.dao.Message
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.core.types.MessageType
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.MessageList
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import java.util.concurrent.TimeUnit

object MessageService {
    private const val TAG = "MessageService"
    private val gson: Gson = Gson()


    fun loadMessagesAndSaveToLocal(olderThan: String?, page: Int, limit: Int): Completable {
        return GWApiManager.shared().listMessage(null, olderThan, null, page, limit)
            .flatMapCompletable { data ->
                Completable.fromAction {
                    var messages: MessageList = gson.fromJson(data, MessageList::class.java)
                    Log.d(TAG, "服务器信息长度:${messages.items.size}")
                    saveMessagesToLocal(messages)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .timeout(10, TimeUnit.SECONDS)
            .onErrorComplete()
            .doOnSubscribe { Log.d(TAG, "开始从服务器加载消息") }
            .doOnComplete { Log.d(TAG, "服务器消息处理完成") }
            .doOnError { error -> Log.e(TAG, "服务器消息处理失败", error) }
    }

    private fun saveMessagesToLocal(messages: MessageList) {
        try {
            val daoCore = ChatSDK.db().getDaoCore()
            val daoSession = daoCore.getDaoSession()
            val convertedMessages = mutableListOf<Message>()
            var sender = ChatSDK.currentUser().id
            for (item in messages.items.reversed()) {
                try {
                    val message = Message().apply {
                        entityID = item.id
//                        text = item.content
                        date = DateLocalizationUtil.parseUTCString(item.createdAt)
                        senderId = sender
                        type = MessageType.Text
                        status = MessageSendStatus.Sent.ordinal
                        threadId = item.sessionId
                    }
                    convertedMessages.add(message)
//                    Log.e(TAG, "src:${item.id},${item.content},${item.createdAt}")
                } catch (e: Exception) {
                    Log.e(TAG, "convertedMessages失败", e)
                }
            }

            // 使用事务批量插入
            daoSession.runInTx {
                val messageDao = daoSession.messageDao
                messageDao.insertOrReplaceInTx(convertedMessages)
            }

            var total = messages.items.size - 1
            convertedMessages.forEachIndexed { i, m ->
                var detail = messages.items[total - i]
                m.text = detail.content
                m.setMetaValue(GWThreadHandler.KEY_AI_FEEDBACK, gson.toJson(detail))
//                Log.e(TAG, "dst:${m.entityID},${detail.content}")
            }

            Log.d(TAG, "✅ 成功保存 ${convertedMessages.size} 条消息到本地数据库")

        } catch (e: Exception) {
            Log.e(TAG, "保存消息到数据库失败", e)
        }
    }
}