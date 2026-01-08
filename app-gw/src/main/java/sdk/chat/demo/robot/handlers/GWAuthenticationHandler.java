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
        return null;
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
//        return Completable.create(emitter -> {
//
//            ChatSDK.events().source().accept(NetworkEvent.logout());
////            accessToken = null;
//            clearCurrentUserEntityID();
//            ChatSDK.shared().getKeyStorage().clear();
//
//            ChatSDK.db().closeDatabase();
//
//            emitter.onComplete();
//        }).subscribeOn(RX.computation());
        return null;
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
        return AuthService.INSTANCE.isAuthenticated(null);
    }


}
