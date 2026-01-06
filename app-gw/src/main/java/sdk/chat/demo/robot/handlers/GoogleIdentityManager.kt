package sdk.chat.demo.robot.handlers

import android.content.Context
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException


object GoogleIdentityManager {

    data class GoogleIdConfig(
        val serverClientId: String,        // Web 客户端 ID
        val clientId: String,              // Android 客户端 ID
        val nonce: String? = null,         // 可选的 nonce，用于防止重放攻击
        val filterByAuthorizedAccounts: Boolean = false, // 只显示已授权的账户
        val associateLinkedAccounts: Boolean = false,    // 关联已链接的账户
        val requestVerifiedPhoneNumber: Boolean = false, // 请求已验证的手机号
        val autoSelectEnabled: Boolean = false           // 自动选择账户
    )

    data class GoogleUserInfo(
        val id: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String?,
        val idToken: String,
        val phoneNumber: String? = null,
    )

    // 生成安全的 nonce
    fun generateNonce(): String {
        val secureRandom = java.security.SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    private const val TAG = "AuthService"
    private const val CLIENT_ID = "518694113094-q7ekgnqqo6mkt6ft0jf2pe40391a3agi.apps.googleusercontent.com"

//    companion object {
//
//        // 创建默认配置
//        fun createDefaultConfig(
//            context: Context,
//            serverClientId: String
//        ): GoogleIdConfig {
//            return GoogleIdConfig(
//                serverClientId = serverClientId,
//                clientId = serverClientId, // 通常与 serverClientId 相同
//                nonce = generateNonce(),
//                filterByAuthorizedAccounts = false,
//                associateLinkedAccounts = false,
//                requestVerifiedPhoneNumber = false,
//                autoSelectEnabled = false
//            )
//        }
//
//    }
    /**
     * 创建 GetGoogleIdOption
     */
    fun createGetGoogleIdOption(): GetGoogleIdOption {
        val builder = GetGoogleIdOption.Builder()
            .setServerClientId(CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)

//        // 设置 nonce（可选）
//        config.nonce?.let { nonce ->
//            builder.setNonce(nonce)
//        }
//
//        // 设置请求手机号（可选）
//        if (config.requestVerifiedPhoneNumber) {
//            builder.setRequestVerifiedPhoneNumber(true)
//        }

        return builder.build()
    }

    /**
     * 获取 Google ID 凭据
     */
    suspend fun getGoogleIdCredential(context:Context): Result<GoogleUserInfo> {
        return try {
//            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
//                .setFilterByAuthorizedAccounts(false)
//                .setServerClientId(CLIENT_ID)
////                .setAutoSelectEnabled(true)
//                // nonce string to use when generating a Google ID token
////                .setNonce(nonce)
//                .build()

            val googleIdOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = CLIENT_ID)
//                .setNonce(nonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
//            handleSignIn(result)
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(result.credential.data)
            val userInfo = parseGoogleIdCredential(googleIdTokenCredential)

            Log.d(TAG, "Google ID 登录成功: ${userInfo.email}")
            Result.success(userInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Google ID 登录失败", e)
            Result.failure(e)
        }
    }

    fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential
        val responseJson: String

        when (credential) {

//            // Passkey credential
//            is PublicKeyCredential -> {
//                // Share responseJson such as a GetCredentialResponse to your server to validate and
//                // authenticate
//                responseJson = credential.authenticationResponseJson
//            }
//
//            // Password credential
//            is PasswordCredential -> {
//                // Send ID and password to your server to validate and authenticate.
//                val username = credential.id
//                val password = credential.password
//            }

            // GoogleIdToken credential
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract the ID to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        // You can use the members of googleIdTokenCredential directly for UX
                        // purposes, but don't use them to store or control access to user
                        // data. For that you first need to validate the token:
                        // pass googleIdTokenCredential.getIdToken() to the backend server.
                        // see [validation instructions](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }
//    /**
//     * 静默登录（如果已有账户）
//     */
//    suspend fun silentSignIn(): Result<GoogleUserInfo> {
//        return try {
//            val googleId = GoogleId.getSignInClient(context)
//            val getGoogleIdOption = createGetGoogleIdOption()
//
//            // 尝试静默获取凭据
//            val googleIdTokenCredential = googleId.getSignInCredential(getGoogleIdOption).await()
//
//            val userInfo = parseGoogleIdCredential(googleIdTokenCredential)
//
//            Log.d(TAG, "静默登录成功: ${userInfo.email}")
//            Result.success(userInfo)
//
//        } catch (e: Exception) {
//            Log.w(TAG, "静默登录失败", e)
//            Result.failure(e)
//        }
//    }

//    /**
//     * 登出
//     */
//    suspend fun signOut(): Result<Unit> {
//        return try {
//            val googleId = GoogleId.getSignInClient(context)
//            googleId.signOut().await()
//
//            Log.d(TAG, "登出成功")
//            Result.success(Unit)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "登出失败", e)
//            Result.failure(e)
//        }
//    }

//    /**
//     * 撤销访问
//     */
//    suspend fun revokeAccess(): Result<Unit> {
//        return try {
//            val googleId = GoogleId.getSignInClient(context)
//            googleId.revokeAccess().await()
//
//            Log.d(TAG, "撤销访问成功")
//            Result.success(Unit)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "撤销访问失败", e)
//            Result.failure(e)
//        }
//    }

    /**
     * 解析 Google ID 凭据
     */
    private fun parseGoogleIdCredential(
        credential: GoogleIdTokenCredential
    ): GoogleUserInfo {
        return GoogleUserInfo(
            id = credential.id,
            email = credential.id,
            displayName = credential.displayName,
            photoUrl = credential.profilePictureUri?.toString(),
            idToken = credential.idToken,
            phoneNumber = credential.phoneNumber
        )
    }

    /**
     * 创建特定场景的 GetGoogleIdOption
     */
    fun createOptionForScenario(scenario: SignInScenario): GetGoogleIdOption {
        return when (scenario) {
            SignInScenario.DEFAULT -> createGetGoogleIdOption()
            SignInScenario.ONE_TAP -> createOneTapOption()
            SignInScenario.PASSWORD -> createPasswordOption()
            SignInScenario.AUTO_SELECT -> createAutoSelectOption()
        }
    }

    private fun createOneTapOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
    }

    private fun createPasswordOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
    }

    private fun createAutoSelectOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
    }

    enum class SignInScenario {
        DEFAULT,     // 默认登录
        ONE_TAP,     // One Tap 登录
        PASSWORD,    // 密码登录
        AUTO_SELECT  // 自动选择账户
    }
}