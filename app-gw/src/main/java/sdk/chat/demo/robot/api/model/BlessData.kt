package sdk.chat.demo.robot.api.model
import com.google.gson.annotations.SerializedName

data class BlessData(
    val choices: List<String>,
    val daily: List<ImageDaily>,
    val font: String,
    @SerializedName("share_text")
    var shareText: String
)

data class DailyItem(
    val background: String,
    val date: String,
    val reference: String,
    val verse: String
)
