package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName

data class ApiTokenResponse(
    @SerializedName("access_token")
    val accessToken: String = "",
    @SerializedName("refresh_token")
    val refreshToken: String = "",
    @SerializedName("expires_in")
    val expiresIn: Int = 0,

    @SerializedName("token_type")
    val tokenType: String = "Bearer",

    @SerializedName("user")
    val user: UserInfo = UserInfo()
){
    // 初始化时计算并存储
    var fullAccessToken: String = ""
    var expiresAt: Long = 0

    fun initData(){
        fullAccessToken= "$tokenType $accessToken".trim()
        expiresAt = System.currentTimeMillis() + expiresIn
    }

    /**
     * 检查令牌是否有效
     */
    val isValid: Boolean
        get() = accessToken.isNotBlank() && tokenType.isNotBlank()


    // 计算属性：检查是否即将过期
    val isExpiringSoon: Boolean
        get() = (expiresAt - System.currentTimeMillis()) <= 300

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}

data class UserInfo(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("guest")
    val isGuest: Boolean = false,

    @SerializedName("display_name")
    val displayName: String = "",

    @SerializedName("avatar_url")
    val avatarUrl: String = "",

    @SerializedName("membership_active")
    val membershipActive: Boolean = false,

    @SerializedName("membership_expired_at")
    val membershipExpiredAt: Long = 0L
)