package sdk.chat.demo.robot.api;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Date;
import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.core.dao.Message;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.AccountDetails;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.BuildConfig;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.api.model.FavoriteList;
import sdk.chat.demo.robot.api.model.SystemConf;
import sdk.chat.demo.robot.api.model.TaskProgress;
import sdk.chat.demo.robot.extensions.LanguageUtils;
import sdk.chat.demo.robot.handlers.BillingManager;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.push.UpdateTokenWorker;
import sdk.guru.common.RX;

//mysql -h 172.17.0.3 -u root coze_data -p

public class GWApiManager {
    private final Gson gson = new Gson();
    private final OkHttpClient client;
    private String accessToken;
    //    private final static String URL = "https://api-test.grace-word.com/api/";
//    private final static String URL = "https://api.grace-word.com/api/";
    public final static int contentTypeUser = 1;
    public final static int contentTypeAI = 2;
    public final static String timeZoneId = TimeZone.getDefault().getID();

    private final static GWApiManager instance = new GWApiManager();

    public static GWApiManager shared() {
        return instance;
    }

    private final static String URL;

    static {

        if (BuildConfig.DEBUG) {
            URL = "https://api-test.grace-word.com/api/";
//            URL = "http://8.217.172.116:5000/api/";
//            URL = "https://api.grace-word.com/api/";
        } else {
            URL = "https://api.grace-word.com/api/";
        }
    }

    private final static String URL_LOGIN = URL + "auth/login";
    private final static String URL_SESSION = URL + "session";
    private final static String URL_MESSAGE = URL + "message";
    private final static String URL_CONF = URL + "system/conf";
    private final static String URL_FAVORITE = URL + "favorite";

    protected GWApiManager() {

    }

    {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Request request = chain.request();

                        // 添加全局查询参数
                        HttpUrl.Builder urlBuilder = request.url().newBuilder();
                        urlBuilder.addQueryParameter("tz", timeZoneId);
                        urlBuilder.addQueryParameter("lang", LanguageUtils.INSTANCE.getAppLanguage(MainApp.getContext(), false));

                        // 构建新请求
                        Request newRequest = request.newBuilder()
                                .url(urlBuilder.build())
                                .build();

                        return chain.proceed(newRequest);
                    }
                })
                .addInterceptor(new CommonHeadersInterceptor())
                .addInterceptor(new TokenRefreshInterceptor())
                .addInterceptor(new ErrorClassifierInterceptor()) // 然后添加错误分类拦截器
//                .addNetworkInterceptor(new SelectiveDiskCacheInterceptor())
                .build();
    }

    public OkHttpClient getClient() {
        return client;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @SuppressLint("CheckResult")
    public String refreshTokenSync() {
        accessToken = null;
        authenticate(ChatSDK.auth().cachedAccountDetails()).blockingGet();
        return accessToken;
    }

    public static Request buildPostRequest(Map<String, String> params, String url) {
        String gsonData = new JSONObject(params).toString();

        RequestBody body = RequestBody.create(
                gsonData,
                MediaType.parse("application/json; charset=utf-8")
        );

        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }

    public <T> T handleResponse(Response response,Class<T> classOfT) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("HTTP " + response.code() + ": " + response.message());
        }

        String responseBody = response.body() != null ? response.body().string() : "";
        if (responseBody.isEmpty()) {
            throw new IOException("Empty response body");
        }

        JsonObject jsonObject;
        try {
            jsonObject = gson.fromJson(responseBody, JsonObject.class);
        } catch (Exception e) {
            throw new IOException("Invalid JSON response: " + e.getMessage());
        }

        JsonPrimitive codePrimitive = jsonObject.getAsJsonPrimitive("code");
        String code = codePrimitive != null ? codePrimitive.getAsString() : null;
        if (!"OK".equals(code)) {
            String errorMessage = jsonObject.getAsJsonPrimitive("message").getAsString();
            throw new IOException(errorMessage);
        } else {
            JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
            if(classOfT==null||classOfT==JsonObject.class){
                return (T) data;
            }else{
                return gson.fromJson(data, classOfT);
            }
        }

    }

    public Single<AccountDetails> authenticate(final AccountDetails details) {
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
            String gsonData = new JSONObject(params).toString();

            RequestBody body = RequestBody.create(
                    gsonData,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_LOGIN)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException error) {
                    System.err.println("请求失败: " + error.getMessage());
                    emitter.onError(error);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    JsonObject resp = gson.fromJson(response.body().string(), JsonObject.class);
                    try {
                        if (resp != null && !resp.get("success").getAsBoolean()) {
                            throw new Exception("login failed:" + resp.get("message").getAsString());
                        }
                        JsonObject data = resp.getAsJsonObject("data");
                        accessToken = "Bearer " + data.get("access_token").getAsString();
                        details.setMetaValue("userId", data.get("user_id").getAsString());
                        int expiredAt = 0;
                        if(data.has("membership_expired_at")){
                            expiredAt = data.get("membership_expired_at").getAsInt();
                            if(expiredAt>0){
                                BillingManager.Companion.getInstance().setExpiredAt(expiredAt* 1000L);
                            }
//                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            String expiredAtStr = sdf.format(new Date(System.currentTimeMillis()+expiredAt*1000));
//                            Log.e("BillingManager","expiredAtStr:"+expiredAtStr+","+expiredAt);
                        }
                        Logger.error("BillingManager: expiredAt "+expiredAt);
                        emitter.onSuccess(details);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }
            });
        }).subscribeOn(RX.io());
    }

    public Single<JsonObject> saveSession(String robotId) {
        return Single.create(emitter -> {
            Map<String, String> params = new HashMap<>();
            params.put("robot_id", robotId);

            Type typeObject = new TypeToken<HashMap>() {
            }.getType();
            String gsonData = gson.toJson(params, typeObject);
            RequestBody body = RequestBody.create(
                    gsonData,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_SESSION)
                    .post(body)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public Single<Boolean> setSummary(String msgId, String summary) {

        return Single.create(emitter -> {
            Map<String, String> params = new HashMap<String, String>();
            params.put("summary", summary);
//            RequestBody body = RequestBody.create(
//                    new JSONObject(params).toString(),
//                    MediaType.parse("application/json; charset=utf-8")
//            );
//
//            Request request = new Request.Builder()
//                    .url(URL_MESSAGE + "/" + msgId)
////                    .header("Authorization", accessToken)
//                    .post(body)
//                    .build();
            Request request = buildPostRequest(params, URL_MESSAGE + "/" + msgId);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException error) {
                    emitter.onError(new Exception("send msg error"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code() + "," + responseBody));
                            return;
                        }
                        JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
                        try {
                            if (resp != null && !resp.get("success").getAsBoolean()) {
                                throw new Exception("login failed:" + resp.get("message").getAsString());
                            }
                            emitter.onSuccess(Boolean.TRUE);
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }

                }
            });

        });
    }


    public Single<Long> setMsgSession(String msgId, Long sessionId, String sessionName) {

        return Single.create(emitter -> {
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("session_id", sessionId);
            params.put("session_name", sessionName);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_MESSAGE + "/" + msgId)
//                    .header("Authorization", accessToken)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException error) {
                    emitter.onError(new Exception("send msg error"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code() + "," + responseBody));
                            return;
                        }
                        JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
                        try {
                            if (resp == null || !resp.get("success").getAsBoolean()) {
                                throw new Exception("setSession failed:" + responseBody);
                            }
                            JsonObject data = resp.getAsJsonObject("data");
                            if (sessionId == -1) {
                                emitter.onSuccess(-1L);
                            } else {
                                emitter.onSuccess(data.get("session_id").getAsLong());
                            }
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }

                }
            });

        });
    }


    public Single<Boolean> setSessionName(Long sessionId, String sessionName) {

        return Single.create(emitter -> {
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("session_name", sessionName);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_SESSION + "/" + sessionId)
//                    .header("Authorization", accessToken)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException error) {
                    emitter.onError(new Exception("call api failed"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code() + "," + responseBody));
                            return;
                        }
                        JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
                        try {
                            if (resp == null || !resp.get("success").getAsBoolean()) {
                                emitter.onError(new Exception("set sessionname failed"));
                            }
                            emitter.onSuccess(Boolean.TRUE);
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }

                }
            });

        });
    }


    public Single<Boolean> renew(String messageId, String prompt) {
        return Single.create(emitter -> {

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("message_id", messageId);
            params.put("prompt", prompt);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_MESSAGE + "/renew")
                    .post(body)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Boolean data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("success").getAsBoolean();
//                        Integer data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data").getAsInt();
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public Single<JsonObject> askRobot(Message message, String prompt) {
        return Single.create(emitter -> {
            Map<String, String> params = message.getMetaValuesAsMap();
            if (prompt != null && !prompt.isEmpty()) {
                params.put("prompt", prompt);
            }
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_MESSAGE)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException error) {
                    message.setMessageStatus(MessageSendStatus.Failed, true);
                    emitter.onError(new Exception(error.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            message.setMessageStatus(MessageSendStatus.Failed, true);
                            emitter.onError(new IOException("HTTP error: " + response.code() + "," + responseBody));
                            return;
                        }
//                        ChatSDK.thread().sendLocalSystemMessage("获取回复中...",message.getThread());
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        message.setMessageStatus(MessageSendStatus.Failed, true);
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }

                }
            });

        });

    }

    public Single<JsonObject> listSession(int page, int limit) {
        return Single.create(emitter -> {


            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_SESSION))
                    .newBuilder()
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("limit", Integer.toString(limit))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }

    public void logout() {
        accessToken = null;
    }

    public Single<JsonObject> getMessageDetail(String contextId, int retry, int stop) {
        return Single.create(emitter -> {

            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_MESSAGE + "/" + contextId))
                    .newBuilder()
                    .addQueryParameter("retry", Integer.toString(retry))
                    .addQueryParameter("stop", Integer.toString(stop))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public Single<SystemConf> getConf() {
        return Single.create(emitter -> {
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_CONF))
                    .newBuilder()
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        SystemConf tagList = gson.fromJson(data, SystemConf.class);
                        emitter.onSuccess(tagList); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<FavoriteList> listFavorite(int page, int limit) {
        return Single.create(emitter -> {


            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_FAVORITE))
                    .newBuilder()
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("limit", Integer.toString(limit))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        FavoriteList res = gson.fromJson(data, FavoriteList.class);
                        emitter.onSuccess(res); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<FavoriteList> listMessage(String sessionType, String search, int page, int limit) {
        return Single.create(emitter -> {

            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_MESSAGE + "/filter"))
                    .newBuilder()
                    .addQueryParameter("session_type", sessionType)
                    .addQueryParameter("search", search)
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("limit", Integer.toString(limit))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        FavoriteList res = gson.fromJson(data, FavoriteList.class);
                        emitter.onSuccess(res); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<Integer> toggleFavorite(String messageId, int contentType) {
        return Single.create(emitter -> {

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("message_id", messageId);
            params.put("content_type", contentType);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_FAVORITE + "/toggle")
                    .post(body)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Integer data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("data").getAsInt();
//                        Integer data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data").getAsInt();
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<Boolean> clearMsg(String messageId, int contentType) {
        return Single.create(emitter -> {

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("message_id", messageId);
            params.put("content_type", contentType);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_MESSAGE + "/del")
                    .post(body)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Boolean data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("success").getAsBoolean();
//                        Integer data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data").getAsInt();
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<Boolean> deleteSession(String threadId) {
        return Single.create(emitter -> {

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("session_id", threadId);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_SESSION + "/del")
                    .post(body)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Boolean data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("success").getAsBoolean();
//                        Integer data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data").getAsInt();
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }
}
