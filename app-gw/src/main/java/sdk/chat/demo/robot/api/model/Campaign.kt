package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName

data class Campaign(
    @SerializedName("topic")
    val topic: String,  // 主题标识，如 "2025_christmas"

    @SerializedName("start_date")
    val startDate: String,  // 开始日期，格式 "YYYY-MM-DD"

    @SerializedName("end_date")
    val endDate: String,  // 结束日期，格式 "YYYY-MM-DD"

    @SerializedName("popup")
    val popupConfig: PopupConfig? = null,  // 弹窗配置

    @SerializedName("entry")
    val entryConfig: EntryConfig? = null,  // 入口配置

    @SerializedName("daily_popup")
    val dailyPopupConfig: DailyPopupConfig? = null  // 每日弹窗配置
)

data class PopupConfig(
    @SerializedName("enable")
    val enable: Boolean,  // 是否启用

    @SerializedName("target_url")
    val targetUrl: String,  // 目标链接，如 "graceword://open/card"

    @SerializedName("button_url")
    val buttonUrl: String,  // 按钮文本，如 "马上领取"

    @SerializedName("background_url")
    val backgroundUrl: String  // 背景图片URL
)

data class EntryConfig(
    @SerializedName("enable")
    val enable: Boolean,  // 是否启用

    @SerializedName("icon_url")
    val iconUrl: String,  // 图标URL，支持JSON动画
    // 可能是Lottie动画文件URL

    @SerializedName("target_url")
    val targetUrl: String  // 目标链接
)

data class DailyPopupConfig(
    @SerializedName("enable")
    val enable: Boolean,  // 是否启用

    @SerializedName("target_url")
    val targetUrl: String,  // 目标链接

    @SerializedName("button_url")
    val buttonUrl: String,  // 主按钮文本

    @SerializedName("background_url")
    val backgroundUrl: String,  // 背景图片URL

    @SerializedName("dismiss_button_text")
    val dismissButtonText: String,  // 关闭按钮文本，如 "稍后再看"
    @SerializedName("dismiss_button_color")
    val dismissButtonTextColor: String
)