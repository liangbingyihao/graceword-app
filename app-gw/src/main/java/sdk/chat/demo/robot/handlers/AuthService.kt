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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.tinylog.Logger
import sdk.chat.core.dao.Keys
import sdk.chat.core.dao.User
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.api.model.ActionLimitConfig.loadDefaultConfigs
import sdk.chat.demo.robot.api.model.ApiTokenResponse
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.extensions.DeviceIdHelper
import sdk.chat.demo.robot.handlers.BillingManager.Companion.getInstance
import sdk.chat.demo.robot.handlers.LimitCounter.initialize
import sdk.chat.demo.robot.push.UpdateTokenWorker
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.Callable

object AuthService {
    var authenticating: Completable? = null
    var loggingOut: Completable? = null
    private var currentUserID: String? = null
    private var isAuthenticatedThisSession: Boolean = false
    private var authDetail: ApiTokenResponse? = null
    private var expiredAt: Long = 0
    private val gson: Gson = Gson()
    private const val KEY_CACHE_USER_LIST: String = "userList"
    private val URL_LOGIN_DEVICE = GWApiManager.URL_V1 + "auth/device"
    private val URL_REFRESH_TOKEN = GWApiManager.URL_V1 + "auth/token/refresh"
    private const val TAG = "AuthService"

    fun authenticate(): Completable {
        return Completable.defer(Callable {
            synchronized(this) {
                if (isAuthenticated()) {
                    Log.d(TAG, "已认证")
                    return@Callable Completable.complete()
                }
                if (authenticating == null) {
                    authenticating =
                        loginDevice().flatMapCompletable(Function { details: ApiTokenResponse ->
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

    fun isAuthenticated(): Boolean {
        return authDetail != null && !authDetail!!.isExpired
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
            val cachedData = JsonCacheManager.get(MainApp.getContext(), KEY_CACHE_USER_LIST)
            if (cachedData.isNullOrBlank()) {
                return emptyList()
            }
            val type: Type = object : TypeToken<List<ApiTokenResponse>>() {}.type
            return gson.fromJson(cachedData, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateAuth(authList: List<ApiTokenResponse>, auth: ApiTokenResponse) {
        val newList = authList.toMutableList()
        val index = newList.indexOfFirst { it.user.id == auth.user.id }

        if (index != -1) {
            newList.removeAt(index)
        }

        newList.add(auth)
        JsonCacheManager.save(MainApp.getContext(), KEY_CACHE_USER_LIST, gson.toJson(newList))
    }

    private fun loginDevice(): Single<ApiTokenResponse?> {
        return Single.fromCallable<ApiTokenResponse?> {
            val params: MutableMap<String?, String?> = HashMap<String?, String?>()
            var url = URL_LOGIN_DEVICE
            var userList = getCacheAuthList()
            var cacheAuth = authDetail
            if (cacheAuth != null && !cacheAuth.refreshToken.isEmpty()) {
                url = URL_REFRESH_TOKEN
                params.put("refresh_token", cacheAuth.refreshToken)
            } else {
                if (userList.isEmpty() || userList.last().user.isGuest) {
                    params.put("guest", DeviceIdHelper.getDeviceId(MainApp.getContext()))
                } else {
                    //TODO
                }
            }
            val fcmToken = UpdateTokenWorker.checkAndUpdateToken(ChatSDK.ctx())
            params.put("fcm_token", fcmToken)
            params.put("bundle_id", MainApp.getContext().packageName)

            val request = GWApiManager.buildPostRequest(params, url)

            // 使用 .use 自动关闭 Response
            GWApiManager.shared().client.newCall(request).execute().use { response ->

                var ret = GWApiManager.shared().handleResponse<ApiTokenResponse?>(
                    response,
                    ApiTokenResponse::class.java
                )
                ret?.let {
                    it.initData()
                    updateAuth(userList, it)
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
//
//    private fun loginDevice2(): Single<ApiTokenResponse?> {
//        return Single.create<ApiTokenResponse?> { emitter ->
//            val params: MutableMap<String?, String?> = HashMap<String?, String?>()
//            var url = URL_LOGIN_DEVICE
//            var userList = getCacheAuthList()
//            if (authDetail != null) {
//
//            } else {
//                if (userList.isEmpty() || userList.last().user.isGuest) {
//                    params.put("guest", DeviceIdHelper.getDeviceId(MainApp.getContext()))
//                } else {
//                    //TODO
//                }
//            }
//            val fcmToken = UpdateTokenWorker.checkAndUpdateToken(ChatSDK.ctx())
//            params.put("fcm_token", fcmToken)
//            params.put("bundle_id", MainApp.getContext().packageName)
//
//            val request = GWApiManager.buildPostRequest(params, url)
//            GWApiManager.shared().client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) {
//                    throw IOException("${response.code}")
//                }
//            }
//            GWApiManager.shared().client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    if (!emitter.isDisposed) {
//                        emitter.onError(e)
//                    }
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    try {
//                        var ret = GWApiManager.shared().handleResponse<ApiTokenResponse?>(
//                            response,
//                            ApiTokenResponse::class.java
//                        )
//                        ret?.let {
//                            updateAuth(userList, it)
//                            emitter.onSuccess(it)
//                        }
//                    } catch (e: Exception) {
//                        if (!emitter.isDisposed) {
//                            emitter.onError(e)
//                        }
//                    }
//                }
//            })
//        }.subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//    }


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
            expiredAt = System.currentTimeMillis() + (authDetail?.expiresIn ?: 0)

            Log.d("Auth", "login success:$expiredAt")
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