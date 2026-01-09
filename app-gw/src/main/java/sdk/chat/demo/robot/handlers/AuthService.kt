package sdk.chat.demo.robot.handlers

//import sdk.chat.demo.robot.handlers.BillingManager.Companion.getInstance
import android.util.Log
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.CompletableOnSubscribe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.Request
import org.greenrobot.greendao.query.QueryBuilder
import org.tinylog.Logger
import sdk.chat.core.dao.DaoCore
import sdk.chat.core.dao.Message
import sdk.chat.core.dao.MessageDao
import sdk.chat.core.dao.User
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.core.types.MessageType
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.api.model.ActionLimitConfig.loadDefaultConfigs
import sdk.chat.demo.robot.api.model.ApiTokenRequest
import sdk.chat.demo.robot.api.model.ApiTokenResponse
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.api.model.MessageList
import sdk.chat.demo.robot.api.model.UserInfo
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.DeviceIdHelper
import sdk.chat.demo.robot.handlers.LimitCounter.initialize
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import sdk.chat.demo.robot.push.UpdateTokenWorker
import sdk.guru.common.RX
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

object AuthService {
    var authenticating: Completable? = null
    var loggingOut: Completable? = null

    //    private var currentUserID: String? = null
    private var isAuthenticatedThisSession: Boolean = false
    private var authDetail: ApiTokenResponse? = null
    private var authDetailList: List<ApiTokenResponse> = emptyList()

    //    private var expiredAt: Long = 0
    private val gson: Gson = Gson()
    private const val KEY_CACHE_USER_LIST: String = "userList"
    private val URL_LOGIN_DEVICE = GWApiManager.URL_V1 + "auth/device"
    private val URL_LOGIN_GOOGLE = GWApiManager.URL_V1 + "auth/oauth/google"
    private val URL_REFRESH_TOKEN = GWApiManager.URL_V1 + "auth/token/refresh"
    private val URL_USER_PROFILE = GWApiManager.URL_V1 + "user/profile"
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
                // 如果正在认证，返回同一个认证流
                if (authenticating != null) {
                    Log.d(TAG, "已有认证进行中")
                    return@Callable authenticating!!
                }

                authenticating =
                    authorizeUser(authorReq).flatMapCompletable(Function { details: ApiTokenResponse ->
                        this.loginSuccessful(details)
                    })
                        .onErrorResumeNext { error ->
                            // 认证失败时返回错误
                            Completable.error(error)
                        }
                        .cache()
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

    private fun clearAuthorInfo() {
        if (authDetail != null && !authDetail!!.user.isGuest) {
            val targetItem = authDetailList.find { it.user.id == authDetail?.user?.id }
            targetItem?.let { item ->
                item.accessToken = ""
                item.refreshToken = ""
                item.fullAccessToken = ""
            }
            JsonCacheManager.save(
                MainApp.getContext(),
                KEY_CACHE_USER_LIST,
                gson.toJson(authDetailList)
            )
            authDetail = null

        }
    }

    //    @Override
    //    public Boolean isAuthenticated() {
    fun logout(): Completable {
        return Completable.create(CompletableOnSubscribe { emitter: CompletableEmitter? ->
            ChatSDK.events().source().accept(NetworkEvent.logout())
            //            accessToken = null;
            clearAuthorInfo()
            (ChatSDK.auth() as GWAuthenticationHandler).clearCurrentUserEntityID()
            ChatSDK.shared().getKeyStorage().clear()

            ChatSDK.db().closeDatabase()
            emitter!!.onComplete()
        }).subscribeOn(RX.computation())
    }

    fun exitsGoogleId(googleId: String?): Boolean {
        val userList = getCacheAuthList()

        if (userList.isEmpty()) {
            Log.w(TAG, "用户缓存列表为空")
            return false
        }

        return if (googleId.isNullOrEmpty()) {
            userList.indexOfFirst { it -> !it.user.isGuest } >= 0
        } else {
            userList.indexOfFirst { it -> it.req?.googleId == googleId } >= 0
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
                val lastUserId = ChatSDK.auth().getCurrentUserEntityID()?.takeIf { it.isNotEmpty() }

                if (lastUserId == null) {
                    Log.d(TAG, "未找到当前用户ID，当前是退出登录的状态")
                    return userList.firstOrNull { !it.user.isGuest } ?: userList[0]
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
            val params: MutableMap<String?, Any?> = HashMap<String?, Any?>()
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
                    if (dstReq.binding) {
                        params.put("binding", true)
                        dstReq.binding = false
                    }
                }
            }

            if (url == null) {
                throw Exception("No login data")
            }

            val fcmToken = UpdateTokenWorker.checkAndUpdateToken(ChatSDK.ctx())
            params.put("fcm_token", fcmToken)
            params.put("bundle_id", MainApp.getContext().packageName)

            Log.e(TAG, "Login..$url,${dstReq?.binding}")
            val request = GWApiManager.buildPostRequest(params, url)

            // 使用 .use 自动关闭 Response
            GWApiManager.shared().client.newCall(request).execute().use { response ->

                var ret = GWApiManager.shared().handleResponse<ApiTokenResponse?>(
                    response,
                    ApiTokenResponse::class.java
                )
                ret?.let {
                    it.initData(dstReq)
                    Log.e(TAG, "Login..${it.bindingResult}")
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

    private fun setLocalDisplayName(displayName: String) {

        if (authDetail != null && !authDetail!!.user.isGuest) {
            val targetItem = authDetailList.find { it.user.id == authDetail?.user?.id }
            targetItem?.let { item ->
                item.user.displayName = displayName
            }
            JsonCacheManager.save(
                MainApp.getContext(),
                KEY_CACHE_USER_LIST,
                gson.toJson(authDetailList)
            )

        }
    }

    fun setAuthStateToIdle() {
        authenticating = null
        loggingOut = null
    }

    fun isOauthAlreadyLinked(): Boolean {
        if (authDetail != null && authDetail!!.bindingResult == "OAUTH_ALREADY_LINKED") {
            return true
        }
        return false
    }


    fun cancel() {
        authenticating = null
    }

    fun loginSuccessful(details: ApiTokenResponse): Completable? {
        return Completable.defer(Callable {
            //            String userId = details.getMetaValue("userId");
            val userId = "user_" + details.user.id
            initDatabaseByUser(userId)
            ChatSDK.auth().setCurrentUserEntityID(userId)
            authDetail = details

            Log.d(TAG, "login success:${authDetail?.expiresAt},${authDetail?.expiresIn}")
            startAsyncTasks()
            // 初始化计数器
            initialize(MainApp.getContext(), null)
            loadDefaultConfigs()
            setAuthStateToIdle()
            Completable.complete()
        }).andThen(executeSyncTasks())  // 等待 executeSyncTasks 完成
            .doOnComplete {
                Log.d(TAG, "loginSuccessful completed")
            }
    }


    private fun executeSyncTasks(): Completable {
        // 这些任务需要等待结果
        return Completable.merge(
            listOf(
                // 关键任务1：获取用户信息
                ImageApi.getServerConfigs()
                    .doOnSubscribe { Log.d(TAG, "开始获取服务器配置") }
                    .doOnSuccess { configs ->
                        Log.d(TAG, "获取到服务器配置")
                    }
                    .ignoreElement()
                    .timeout(10, TimeUnit.SECONDS)  // 添加超时
                    .onErrorComplete(),
                BillingManager.getInstance().getBillingHelper()
                    .doOnSubscribe { Log.d(TAG, "开始获取账单信息") }
                    .doOnSuccess { Log.d(TAG, "账单信息获取完成") }
                    .timeout(10, TimeUnit.SECONDS)
                    .ignoreElement(),
                Completable.fromAction(Action { (ChatSDK.thread() as GWThreadHandler).createChatSessions() })
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe { Log.d(TAG, "开始创建聊天会话") }
                    .doOnComplete { Log.d(TAG, "聊天会话创建完成") }
                    .timeout(10, TimeUnit.SECONDS)
                    .onErrorComplete(),
                initMessageFromServer()
                    .doOnSubscribe { Log.d(TAG, "开始initMessageFromServer") }
                    .doOnComplete { Log.d(TAG, "initMessageFromServer完成") }
                    .timeout(10, TimeUnit.SECONDS)
                    .onErrorComplete(),
            )
        ).timeout(60, TimeUnit.SECONDS)  // 总体超时
            .doOnSubscribe { Log.d(TAG, "开始执行同步任务") }
            .doOnComplete {
                Log.d(TAG, "所有同步任务完成")
            }
            .doOnError { error ->
                Log.e(TAG, "同步任务执行失败", error)
            }
    }

    private fun startAsyncTasks() {
        // 这些任务会自己启动，不阻塞主流程

        MainApp.addGlobalDisposable(
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
        )
        MainApp.addGlobalDisposable(
            SocialShareHandler.getHeaderImageAsync()
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { Log.d(TAG, "Login log uploaded") },
                    { e -> Log.e(TAG, "Login log upload failed", e) }
                ))

        ImageApi.listImageTags().subscribe()


        (ChatSDK.thread() as GWThreadHandler).triggerNetworkSync()
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

    fun initMessageFromServer(): Completable {
        return Completable.defer {

            val daoCore: DaoCore = ChatSDK.db().getDaoCore()
            val qb: QueryBuilder<Message?> =
                daoCore.getDaoSession().queryBuilder(Message::class.java)
                    .orderDesc(MessageDao.Properties.Id).limit(1)
            val messages: List<Message?>? = qb.list()

            return@defer if (messages?.isNotEmpty() == true) {
                val lastMessage = messages[0]
                val entityId = lastMessage?.entityID

                Log.d(TAG, "最新消息 entityID: $entityId")

                if (entityId != null && entityId.length > 13) {
                    Log.d(TAG, "最新消息 entityID 长度(${entityId.length}) > 13，无需从服务器加载")
                    Completable.complete()
                } else {
                    Log.d(TAG, "最新消息 entityID 长度不足或为空，从服务器加载")
                    MessageService.loadMessagesAndSaveToLocal(null, 1, 200)
//                    loadMessagesFromServer()
                }
            } else {
                Log.d(TAG, "本地无消息，从服务器加载")
                MessageService.loadMessagesAndSaveToLocal(null, 1, 200)
//                loadMessagesFromServer()
            }
        }

    }


    fun setDisplayName(displayName: String): Completable {
        return Completable.create { emitter ->
            try {
                if (displayName.isBlank()) {
                    val error = IllegalArgumentException("Display name cannot be empty or blank")
                    Log.e(TAG, "setDisplayName: Invalid input", error)
                    emitter.onError(error)
                    return@create
                }
                if(displayName == authDetail?.user?.displayName){
                    emitter.onComplete()
                    return@create
                }

                // 创建 JSON 请求体
                val json = """
                {
                    "display_name": "$displayName"
                }
            """.trimIndent()

                val requestBody =
                    json.toRequestBody("application/json; charset=utf-8".toMediaType())

                // 构建请求
                val requestBuilder = Request.Builder()
                    .url(URL_USER_PROFILE)
                    .patch(requestBody)
                    .addHeader("Content-Type", "application/json")

                val request = requestBuilder.build()

                GWApiManager.shared().client.newCall(request).execute().use { response ->

                    var ret = GWApiManager.shared().handleResponse<JsonObject?>(
                        response,
                        JsonObject::class.java
                    )
                    if (ret != null) {
                        Log.e(TAG, "setDisplayName..done")
                        setLocalDisplayName(displayName)
                        emitter.onComplete()
                    } else {
                        emitter.onError(IOException("set error"))
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "setDisplayName..${e}")
                emitter.onError(e)
            }
        }.subscribeOn(RX.io())
    }
}