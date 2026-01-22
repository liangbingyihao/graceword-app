package sdk.chat.demo.robot.api.model

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import sdk.chat.demo.robot.api.model.FavoriteList.FavoriteItem
import java.lang.reflect.Type

class FavoriteItemDeserializer : JsonDeserializer<FavoriteItem> {
    companion object {
        private val defaultGson = Gson()
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): FavoriteItem {
        val jsonObject = json.asJsonObject

        // 使用 Gson 的默认反序列化创建基础对象
        val favoriteItem = defaultGson.fromJson<FavoriteItem>(json, FavoriteItem::class.java)
//        val favoriteItem = context.deserialize<FavoriteItem>(
//            json,
//            FavoriteItem::class.java
//        )

        // 处理 content 字段
        val contentElement = jsonObject.get("content")
        if (contentElement != null && !contentElement.isJsonNull) {
            try {
                if (contentElement.isJsonPrimitive) {
                    val jsonObject = JsonParser.parseString(contentElement.asString).asJsonObject
                    if (jsonObject.has("hymns")) {
                        val listType = object : TypeToken<MutableList<Song?>?>() {}.getType()
                        favoriteItem.songs =
                            context.deserialize<MutableList<Song?>?>(
                                jsonObject.get(
                                    "hymns"
                                ), listType
                            )
                        if (jsonObject.has("response")) {
                            favoriteItem.content = jsonObject.get("response").asString
                        }
                    } else {
                        favoriteItem.songs = emptyList()
                    }
//
//                        // 尝试将 content 解析为 List<Song>
//                        val songs = defaultGson.fromJson<List<Song>>(
//                            contentString,
//                            object : TypeToken<List<Song>>() {}.type
//                        )
//                        favoriteItem.songs = songs
                }
            } catch (e: Exception) {
                // 解析失败，保持 content 原样
            }
        }

        return favoriteItem
    }

    private fun isValidJson(jsonString: String): Boolean {
        return try {
            JsonParser.parseString(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }
}