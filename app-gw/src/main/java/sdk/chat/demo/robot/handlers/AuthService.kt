package sdk.chat.demo.robot.handlers

import android.util.Log
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.tinylog.Logger
import sdk.chat.core.dao.Keys
import sdk.chat.core.dao.User
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.api.model.ActionLimitConfig.loadDefaultConfigs
import sdk.chat.demo.robot.api.model.ApiTokenRequest
import sdk.chat.demo.robot.api.model.ApiTokenResponse
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.api.model.UserInfo
import sdk.chat.demo.robot.extensions.DeviceIdHelper
import sdk.chat.demo.robot.handlers.BillingManager.Companion.getInstance
import sdk.chat.demo.robot.handlers.LimitCounter.initialize
import sdk.chat.demo.robot.push.UpdateTokenWorker
import java.lang.reflect.Type
import java.util.concurrent.Callable

object AuthService {
    var authenticating: Completable? = null
    var loggingOut: Completable? = null
    private var currentUserID: String? = null
    private var isAuthenticatedThisSession: Boolean = false
    private var authDetail: ApiTokenResponse? = null
    private var authDetailList: List<ApiTokenResponse> = emptyList()
//    private var expiredAt: Long = 0
    private val gson: Gson = Gson()
    private const val KEY_CACHE_USER_LIST: String = "userList"
    private val URL_LOGIN_DEVICE = GWApiManager.URL_V1 + "auth/device"
    private val URL_LOGIN_GOOGLE = GWApiManager.URL_V1 + "auth/oauth/google"
    private val URL_REFRESH_TOKEN = GWApiManager.URL_V1 + "auth/token/refresh"
    private const val TAG = "AuthService"

    fun authenticate(authorReq: ApiTokenRequest? = null): Completable {
        //userinfo为空时，尽量确保最近一次登录的账号登录状态；
        //有值时，确保指定账号的登录状态
        return Completable.defer(Callable {
            synchronized(this) {
                if (isAuthenticated(authorReq)) {
                    Log.d(TAG, "已认证")
                    return@Callable Completable.complete()
                }
                if (authenticating == null) {
                    authenticating =
                        authorizeUser(authorReq).flatMapCompletable(Function { details: ApiTokenResponse ->
                            this.loginSuccessful(details)
                        })
                            .doOnSubscribe {
                                Log.d(TAG, "开始认证流程")
                            }
                            .doOnComplete {
                                Log.d(TAG, "认证成功")
                            }
                            .doOnError { error ->
                                Log.e(TAG, "认证失败", error)
                            }
                            .onErrorResumeNext { error ->
                                // 认证失败时返回错误
                                Completable.error(error)
                            }
                            .cache()
                }
                authenticating ?: Completable.error(NullPointerException("认证流程为空"))
            }
        }).doFinally(Action { this.cancel() })
    }

    fun getLastLoginUser(): UserInfo? {
        var lastAuthor = filterAuthorInfo()
        if (lastAuthor != null) {
            return lastAuthor.user
        }
        return null
    }

    fun logout(authorReq: ApiTokenRequest): Boolean{
        if(!authorReq.googleId.isEmpty()){

        }
        return false
    }

    private fun filterAuthorInfo(customFilter: ((ApiTokenResponse) -> Boolean)? = null): ApiTokenResponse? {
        return try {

            // 2. 获取缓存列表（带异常处理）
            val userList = getCacheAuthList()

            if (userList.isEmpty()) {
                Log.w(TAG, "用户缓存列表为空")
                return null
            }

            Log.d(TAG, "缓存用户列表大小: ${userList.size}")

            // 3. 过滤匹配的用户
            val matchingUsers = if (customFilter != null) {
                // 使用外部提供的过滤函数
                userList.filter { userEntity ->
                    try {
                        customFilter(userEntity)
                    } catch (e: Exception) {
                        Log.e(TAG, "自定义过滤函数执行出错: ${e.message}")
                        false
                    }
                }
            } else {
                // 使用默认过滤逻辑
                // 1. 获取当前用户ID（带空值检查）
                val lastUserId = getCurrentUserEntityID()?.takeIf { it.isNotEmpty() }

                if (lastUserId == null) {
                    Log.d(TAG, "未找到当前用户ID，返回null")
                    return null
                }

                Log.d(TAG, "当前用户ID: $lastUserId")
                userList.filter { userEntity ->
                    try {
                        // 安全地检查用户ID匹配
                        lastUserId.contains(userEntity.user.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "过滤用户时出错: ${e.message}")
                        false
                    }
                }
            }

            // 4. 检查匹配结果
            when (matchingUsers.size) {
                0 -> {
                    Log.w(TAG, "未找到匹配的用户")
                    null
                }

                1 -> {
                    val dstAuthor = matchingUsers[0]
                    Log.d(TAG, "成功找到目标作者: ${dstAuthor.user.id}")
                    dstAuthor
                }

                else -> {
                    Log.e(TAG, "找到多个匹配用户(${matchingUsers.size})")
                    // 如果有多个匹配，返回第一个有效的
                    matchingUsers[0]
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "获取目标作者时发生异常", e)
            null
        }
    }

    private fun authorizeUser(authorReq: ApiTokenRequest? = null): Single<ApiTokenResponse?> {
        return Single.fromCallable<ApiTokenResponse?> {
            val params: MutableMap<String?, String?> = HashMap<String?, String?>()
            val lastRsp = if (authorReq != null) {
                filterAuthorInfo { rsp ->
                    authorReq.getLocalId() == rsp.req?.getLocalId()
                }
            } else {
                filterAuthorInfo()
            }

            var dstReq = authorReq
            var url: String? = null
            if (lastRsp != null && !lastRsp.refreshToken.isEmpty()) {
                url = URL_REFRESH_TOKEN
                dstReq = lastRsp.req
                params.put("refresh_token", lastRsp.refreshToken)
            } else {
                if (dstReq == null) {
                    dstReq =
                        ApiTokenRequest(guest = DeviceIdHelper.getDeviceId(MainApp.getContext()))
                }

                if (!dstReq.guest.isEmpty()) {
                    url = URL_LOGIN_DEVICE
                    params.put("guest", DeviceIdHelper.getDeviceId(MainApp.getContext()))
                } else if (!dstReq.googleToken.isEmpty()) {
                    url = URL_LOGIN_GOOGLE
                    params.put("id_token", dstReq.googleToken)
                }
            }

            if (url == null) {
                throw Exception("No login data")
            }

            val fcmToken = UpdateTokenWorker.checkAndUpdateToken(ChatSDK.ctx())
            params.put("fcm_token", fcmToken)
            params.put("bundle_id", MainApp.getContext().packageName)

            Log.e(TAG, "Login..$url,$params:")
            val request = GWApiManager.buildPostRequest(params, url)

            // 使用 .use 自动关闭 Response
            GWApiManager.shared().client.newCall(request).execute().use { response ->

                var ret = GWApiManager.shared().handleResponse<ApiTokenResponse?>(
                    response,
                    ApiTokenResponse::class.java
                )
                ret?.let {
                    it.initData(dstReq)
                    updateAuth(it)
                    return@use it
                }

                return@use null
            }
        }
            .onErrorResumeNext { error ->
                // 统一错误处理
                Log.e(TAG, "Login device error", error)
                Single.error(error)
            }
            .subscribeOn(Schedulers.io())
    }

    fun isAuthenticated(authorReq: ApiTokenRequest? = null): Boolean {
        var ret = authDetail != null && !authDetail!!.isExpired
        if (authorReq != null && ret) {
            ret = authDetail?.req?.getLocalId() == authorReq.getLocalId()
        }
        return ret
    }

    fun getAccessToken(): String {
        return if (isAuthenticated()) {
            authDetail?.fullAccessToken ?: ""
        } else {
            ""
        }
    }

    fun getCacheAuthList(): List<ApiTokenResponse> {
        return try {
            if (authDetailList.isEmpty()) {
                val cachedData = JsonCacheManager.get(MainApp.getContext(), KEY_CACHE_USER_LIST)
                if (cachedData.isNullOrBlank()) {
                    return emptyList()
                }
                val type: Type = object : TypeToken<List<ApiTokenResponse>>() {}.type
                authDetailList = gson.fromJson(cachedData, type) ?: emptyList()
            }
            authDetailList
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateAuth(auth: ApiTokenResponse) {
        val newList = authDetailList.toMutableList()
        val index = newList.indexOfFirst { it.user.id == auth.user.id }

        if (index != -1) {
            newList.removeAt(index)
        }
        newList.add(auth)
        authDetailList = newList
        JsonCacheManager.save(MainApp.getContext(), KEY_CACHE_USER_LIST, gson.toJson(newList))
    }

    fun setCurrentUserEntityID(userID: String?) {
        currentUserID = userID
        isAuthenticatedThisSession = true
        ChatSDK.shared().getKeyStorage().put(Keys.CurrentUserID, currentUserID)
    }

    fun getCurrentUserEntityID(): String? {
        if (currentUserID == null || !isAuthenticated()) {
            currentUserID = ChatSDK.shared().getKeyStorage().get(Keys.CurrentUserID)
        }
        return currentUserID
    }

    fun setAuthStateToIdle() {
        authenticating = null
        loggingOut = null
    }


    fun cancel() {
        authenticating = null
    }

    fun loginSuccessful(details: ApiTokenResponse): Completable? {
        return Completable.defer(Callable {
            //            String userId = details.getMetaValue("userId");
            val userId = "user_" + details.user.id
            initDatabaseByUser(userId)
            setCurrentUserEntityID(userId)
            authDetail = details

            Log.d(TAG, "login success:${authDetail?.expiresAt},${authDetail?.expiresIn}")
//            if (details.type == AccountDetails.Type.Username) {
//                ChatSDK.shared().getKeyStorage().save(details.username, details.password)
//            }
            val handler = ChatSDK.thread() as GWThreadHandler
            ImageApi.getServerConfigs().subscribe()
            getInstance().getBillingHelper().subscribe()
            SocialShareHandler.getHeaderImageAsync().subscribe()
            handler.createChatSessions()
            // 初始化计数器
            initialize(MainApp.getContext(), null)
            loadDefaultConfigs()
            ImageApi.listImageTags().subscribe()
            setAuthStateToIdle()


            ImageApi.listImageDaily(null)
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(Consumer { data: MutableList<ImageDaily?>? ->
                    if (data != null && !data.isEmpty()) {
                        val url = data.get(0)!!.getUrl()
                        Glide.with(MainApp.getContext())
                            .load(url)
                            .preload()
                    }
                })
            Completable.complete()
        })
    }

    fun ensureDatabase() {
        if (!isAuthenticated()) {
            initDatabaseByUser(ChatSDK.currentUserID())
        } else {
            Log.d(TAG, "no need to ensureDatabase")
        }
    }

    fun initDatabaseByUser(userId: String?) {
        if (userId == null || userId.isEmpty()) {
            Logger.error { "ensureDatabase no userId" }
            throw java.lang.Exception("no userId")
        }
        ChatSDK.db().openDatabase(userId)
        val user = ChatSDK.db().fetchOrCreateEntityWithEntityID<User?>(User::class.java, userId)
        val robot = ChatSDK.contact().contacts()
        if (!robot.isEmpty()) {
            user.addContact(robot.get(0))
            ChatSDK.db().update(user)
        }
        Log.d(TAG, "initDatabaseByUser $userId")
        Logger.error { "ensureDatabase done" }
    }
}