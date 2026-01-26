package sdk.chat.demo.robot.api;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
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
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.BuildConfig;
import sdk.chat.demo.robot.api.model.FavoriteItemDeserializer;
import sdk.chat.demo.robot.api.model.FavoriteList;
import sdk.chat.demo.robot.api.model.MessageList;
import sdk.chat.demo.robot.api.model.SystemConf;
import sdk.chat.demo.robot.extensions.LanguageUtils;
import sdk.chat.demo.robot.handlers.AuthService;

//mysql -h 172.17.0.3 -u root coze_data -p

public class GWApiManager {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(FavoriteList.FavoriteItem.class, new FavoriteItemDeserializer())
            .create();
    private final OkHttpClient client;
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

    public final static String URL_V1;

    static {

        if (BuildConfig.DEBUG) {
            URL = "https://api-test.grace-word.com/api/";
//            URL = "http://8.217.172.116:5000/api/";
//            URL = "https://api.grace-word.com/api/";
        } else {
            URL = "https://api.grace-word.com/api/";
        }
        URL_V1 = URL + "v1/";
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

    //    public String getAccessToken() {
//        return accessToken;
//    }
//
    @SuppressLint("CheckResult")
    public String refreshTokenSync() {
        AuthService.INSTANCE.authenticate(null).blockingGet();
        return AuthService.INSTANCE.getAccessToken();
    }

    public static Request buildPostRequest(Map params, String url) {
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

    public <T> T handleResponse(Response response, Class<T> classOfT) throws IOException {
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
            JsonPrimitive msg = jsonObject.getAsJsonPrimitive("msg");
            String errorMessage = msg != null ? msg.getAsString() : "Unknown error from backend";
            throw new IOException(errorMessage);
        } else {
            JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
            if (classOfT == null || classOfT == JsonObject.class) {
                return (T) data;
            } else {
                return gson.fromJson(data, classOfT);
            }
        }

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
                        SystemConf ret = GWApiManager.shared().handleResponse(response, SystemConf.class);
                        emitter.onSuccess(ret); // 请求成功
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
                        FavoriteList ret = GWApiManager.shared().handleResponse(response, FavoriteList.class);
                        emitter.onSuccess(ret);
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<FavoriteList> searchMessage(String sessionType, String search, int page, int limit) {
        return Single.create(emitter -> {
            if (search == null || search.isEmpty()) {
                emitter.onError(new IOException(" Need search str "));
            }
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
                        FavoriteList ret = GWApiManager.shared().handleResponse(response, FavoriteList.class);
                        emitter.onSuccess(ret);
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<MessageList> listMessage(String olderThan, int page, int limit) {
        return Single.create(emitter -> {
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_MESSAGE))
                    .newBuilder()
                    .addQueryParameter("older_than", olderThan)
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("limit", Integer.toString(limit))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        MessageList ret = GWApiManager.shared().handleResponse(response, MessageList.class);
                        emitter.onSuccess(ret); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

    public Single<MessageList> listSessionMessage(Long sessionId, Long olderThan, int page, int limit) {
        return Single.create(emitter -> {
            HttpUrl.Builder url = Objects.requireNonNull(HttpUrl.parse(URL_SESSION + "/message"))
                    .newBuilder();
            if (sessionId != null) {
                url.addQueryParameter("session_id", Long.toString(sessionId));
            }
            if (olderThan != null && olderThan > 0) {
                url.addQueryParameter("older_than", Long.toString(olderThan));
            }
            url.addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("limit", Integer.toString(limit));

            Request request = new Request.Builder()
                    .url(url.build())
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        MessageList ret = GWApiManager.shared().handleResponse(response, MessageList.class);
                        emitter.onSuccess(ret); // 请求成功
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
