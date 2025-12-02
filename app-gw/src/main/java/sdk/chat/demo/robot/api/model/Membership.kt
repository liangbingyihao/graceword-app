package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName

data class Membership(
    val display: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("expired_at")
    val expiredAt: Long? = 0L
)
