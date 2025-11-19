package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName
import sdk.chat.demo.robot.handlers.SocialShareHandler
import sdk.chat.demo.robot.holder.TextHolder

data class MessageEntry(
    val id: String,
    val role: String,
    @SerializedName("item_key")
    val itemKey: Int?
)


data class ShareRequest(
    @SerializedName("header_image_id")
    val imageId: Int,
    val messages: List<MessageEntry>
)

data class HeaderImageList(
    val list: List<HeaderImage>
)

data class HeaderImage(
    val id: Int,
    val url: String
)

fun createShareRequest(holders: List<TextHolder>): ShareRequest {
    val messages = mutableListOf<MessageEntry>()

    holders.map { holder ->
        var msgId = holder.message.entityID
        if (holder.isSong) {
            // 处理选中的歌曲
            val songs = holder.getSelectedSongIndex()
            if (songs != null && songs.isNotEmpty()) {
                songs.map { songIndex ->
                    messages.add(
                        MessageEntry(
                            id = msgId,
                            role = "assistant",
                            itemKey = songIndex
                        )
                    )
                }
            }
        } else {
            // 处理用户消息
            if (holder.isUserSelected()) {
                messages.add(
                    MessageEntry(
                        id = msgId,
                        role = "user",
                        itemKey = null
                    )
                )
            }

            // 处理AI回复
            if (holder.isAiSelected()) {
                messages.add(
                    MessageEntry(
                        id = msgId,
                        role = "assistant",
                        itemKey = null
                    )
                )
            }

        }
    }

    return ShareRequest(
        imageId = SocialShareHandler.getHeaderImage().id, // 默认图片ID，可以根据需要调整
        messages = messages
    )
}
//fun createBatchLogsRequest(
//    topic: String,
//    description: String,
//    uid: String,
//    logs: String
//): LogRequest {
//    return LogRequest(
//        logstore = "feedback",
//        topic = topic,
//        logs = mutableListOf(
//            LogEntry(
//                timestamp = System.currentTimeMillis() / 1000,
//                kv = mutableListOf(
//                    KeyValuePair("des", description),
//                    KeyValuePair("uid", uid),
//                    KeyValuePair("logData", logs)
//                )
//            )
//        )
//    )
//}
//
//fun createLogRequest(topic: String, kvs: List<KeyValuePair>): LogRequest {
//    return LogRequest(
//        logstore = "report",
//        topic = topic,
//        logs = mutableListOf(
//            LogEntry(
//                timestamp = System.currentTimeMillis() / 1000,
//                kv = kvs
//            )
//        )
//    )
//}