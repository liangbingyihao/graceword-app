package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;

import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.core.base.AbstractAuthenticationHandler;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.AccountDetails;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.ActionLimitConfig;
import sdk.chat.demo.robot.api.model.ApiTokenResponse;
import sdk.chat.demo.robot.extensions.DeviceIdHelper;
import sdk.chat.demo.robot.push.UpdateTokenWorker;
import sdk.guru.common.RX;


public class GWAuthenticationHandler extends AbstractAuthenticationHandler {
    public GWAuthenticationHandler() {
    }

    @Override
    public Boolean accountTypeEnabled(AccountDetails.Type type) {
        return type == AccountDetails.Type.Username || type == AccountDetails.Type.Register
                || type == AccountDetails.Type.Anonymous;
    }

    @Override
    public Completable authenticate() {
        return AuthService.INSTANCE.authenticate();
//        return Completable.defer(() -> {
//
//            if (isAuthenticatedThisSession() || isAuthenticated()) {
//                return Completable.complete();
//            }
//            if (!isAuthenticating()) {
//                AccountDetails details = cachedAccountDetails();
//                return authenticate(details);
//            }
//            return authenticating;
//        });
    }

    @Override
    public Completable authenticate(final AccountDetails details) {
//        return Completable.defer(() -> {
//            if (!isAuthenticating()) {
//                authenticating = loginDevice(details)
//                        .flatMapCompletable(this::loginSuccessful)
//                        .cache();
//            }
//            return authenticating;
//        }).doFinally(this::cancel);
        return null;
    }

//    public static void ensureDatabase() throws Exception {
//        if (!AuthService.INSTANCE.isAuthenticated()) {
//            initDatabaseByUser(ChatSDK.currentUserID());
//        }
//    }

//    public static void initDatabaseByUser(String userId) throws Exception {
//        if (userId == null || userId.isEmpty()) {
//            Logger.error("ensureDatabase no userId");
//            throw new Exception("no userId");
//        }
//        ChatSDK.db().openDatabase(userId);
//        User user = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, userId);
//        List<User> robot = ChatSDK.contact().contacts();
//        if (!robot.isEmpty()) {
//            user.addContact(robot.get(0));
//            ChatSDK.db().update(user);
//        }
//        Logger.error("ensureDatabase done");
//    }

//    @SuppressLint("CheckResult")
//    protected Completable loginSuccessful(AccountDetails details) {
//        return Completable.defer(() -> {
////            String userId = details.getMetaValue("userId");
//            String userId = "user_" + details.getMetaValue("userId");
//            initDatabaseByUser(userId);
//            setCurrentUserEntityID(userId);
//
//            if (details.type == AccountDetails.Type.Username) {
//                ChatSDK.shared().getKeyStorage().save(details.username, details.password);
//            }
//            GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
//            ImageApi.getServerConfigs().subscribe();
//            BillingManager.Companion.getInstance().getBillingHelper().subscribe();
//            SocialShareHandler.getHeaderImageAsync().subscribe();
//            handler.createChatSessions();
//            // 初始化计数器
//            LimitCounter.INSTANCE.initialize(MainApp.getContext(), null);
//            ActionLimitConfig.INSTANCE.loadDefaultConfigs();
//            ImageApi.listImageTags().subscribe();
//            setAuthStateToIdle();
//
//
//            ImageApi.listImageDaily(null)
//                    .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
//                    .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
//                    .subscribe(data -> {
//                        if (data != null && !data.isEmpty()) {
//                            String url = data.get(0).getUrl();
//                            Glide.with(MainApp.getContext())
//                                    .load(url)
//                                    .preload();
//                        }
//                    });
//
//            return Completable.complete();
//        });
//    }

    public AccountDetails cachedAccountDetails() {
//        AccountDetails accountDetails = AccountDetails.username(ChatSDK.shared().getKeyStorage().get(KeyStorage.UsernameKey), ChatSDK.shared().getKeyStorage().get(KeyStorage.PasswordKey));
//        if (!accountDetails.areValid()) {
//            accountDetails = AccountDetails.token(DeviceIdHelper.INSTANCE.getDeviceId(ChatSDK.ctx()));
//        }
        return AccountDetails.token(DeviceIdHelper.INSTANCE.getDeviceId(ChatSDK.ctx()));
    }

    public Boolean cachedCredentialsAvailable() {
        return true;
    }

//    @Override
//    public Boolean isAuthenticated() {

    /// /        XMPPConnection connection = XMPPManager.shared().getConnection();
//        return GWApiManager.shared().isAuthenticated();
//    }
    @Override
    public Completable logout() {
        return Completable.create(emitter -> {

            ChatSDK.events().source().accept(NetworkEvent.logout());
//            accessToken = null;
            clearCurrentUserEntityID();
            ChatSDK.shared().getKeyStorage().clear();

            ChatSDK.db().closeDatabase();

            emitter.onComplete();
        }).subscribeOn(RX.computation());
    }

    // TODO: Implement this
    @Override
    public Completable changePassword(String email, String oldPassword, String newPassword) {
        return Completable.create(emitter -> {
//            XMPPManager.shared().accountManager().changePassword(newPassword);
            emitter.onComplete();
        }).subscribeOn(RX.io());
    }

    @Override
    public Completable sendPasswordResetMail(String email) {
        return Completable.error(new Throwable("Password email not supported"));
    }

    public Boolean isAuthenticated() {
        return AuthService.INSTANCE.isAuthenticated();
    }

    private final String URL_LOGIN_DEVICE = GWApiManager.URL_V1 + "auth/device";

    private Single<AccountDetails> loginDevice(final AccountDetails details) {
        return Single.create((SingleOnSubscribe<AccountDetails>) emitter -> {
            Map<String, String> params = new HashMap<>();
            if (details.type == AccountDetails.Type.Username) {
                params.put("username", details.username);
                params.put("password", details.password);
            } else if (details.type == AccountDetails.Type.Custom) {
                params.put("guest", details.token);
            } else {
                emitter.onError(new Exception("login type error"));
            }
            String fcmToken = UpdateTokenWorker.checkAndUpdateToken(ChatSDK.ctx());
            params.put("fcmToken", fcmToken);

            Request request = GWApiManager.buildPostRequest(params, URL_LOGIN_DEVICE);

            GWApiManager.shared().getClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException error) {
                    emitter.onError(error);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        ApiTokenResponse jsonObject = GWApiManager.shared().handleResponse(response, ApiTokenResponse.class);
                        details.setMetaValue("userId", jsonObject.getUser().getId());
                        emitter.onSuccess(details);
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        if (response != null) {
                            response.close();
                        }
                    }

//                    JsonObject resp = gson.fromJson(response.body().string(), JsonObject.class);
//                    try {
//                        if (resp != null && !resp.get("success").getAsBoolean()) {
//                            throw new Exception("login failed:" + resp.get("message").getAsString());
//                        }
//                        JsonObject data = resp.getAsJsonObject("data");
//                        accessToken = "Bearer " + data.get("access_token").getAsString();
//                        int expiredAt = 0;
//                        if(data.has("membership_expired_at")){
//                            expiredAt = data.get("membership_expired_at").getAsInt();
//                            if(expiredAt>0){
//                                BillingManager.Companion.getInstance().setExpiredAt(expiredAt* 1000L);
//                            }
////                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
////                            String expiredAtStr = sdf.format(new Date(System.currentTimeMillis()+expiredAt*1000));
////                            Log.e("BillingManager","expiredAtStr:"+expiredAtStr+","+expiredAt);
//                        }
//                        Logger.error("BillingManager: expiredAt "+expiredAt);
//                        emitter.onSuccess(details);
//                    } catch (Exception e) {
//                        emitter.onError(e);
//                    }
                }
            });
        }).subscribeOn(RX.io());
    }

}
