package sdk.chat.demo.robot.handlers;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.JsonCacheManager;
import sdk.chat.demo.robot.api.model.GWConfigs;
import sdk.chat.demo.robot.api.model.HeaderImage;
import sdk.chat.demo.robot.api.model.HeaderImageList;
import sdk.chat.demo.robot.api.model.ShareRequest;

public class SocialShareHandler {
    private static final Gson gson = new Gson();
    private final static String KEY_CACHE_HEADER_IMAGE = "headerImage";
    private final static String URL_SHARE = ImageApi.URL2 + "message/share";
    private final static String URL_SHARE_HEADER = URL_SHARE + "/header-images";
    private static HeaderImage headerImage = null;
    private static final HeaderImage defaultImage = new HeaderImage(1, "https://cdn.grace-word.com/letter/3d340577587b47b9b31f82f7e2fc2a3f.webp");


    public static Single<String> batchShare(ShareRequest shareList) {
        return Single.<String>create(emitter -> {
                    try {
                        // 创建请求体
                        RequestBody body = RequestBody.create(gson.toJson(shareList), MediaType.parse("application/json; charset=utf-8"));

                        // 构建请求
                        Request request = new Request.Builder()
                                .url(URL_SHARE)
                                .post(body)
                                .header("Content-Type", "application/json")
                                .build();

                        OkHttpClient client = GWApiManager.shared().getClient();

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                if (!emitter.isDisposed()) {
                                    emitter.onError(e);
                                }
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try {
                                    JsonObject jsonObject = GWApiManager.shared().handleResponse(response, null);
                                    emitter.onSuccess(jsonObject.getAsJsonPrimitive("url").getAsString());
                                } catch (Exception e) {
                                    emitter.onError(e);
                                } finally {
                                    response.close(); // 关闭 Response
                                }
                            }
                        });
                    } catch (Exception e) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    }
                }
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static HeaderImage getHeaderImage() {
        return headerImage == null ? defaultImage : headerImage;
    }

    private static void setHeaderImage(String data) {
        try {
            if (data == null || data.isEmpty()) {
                data = JsonCacheManager.INSTANCE.get(MainApp.getContext(), KEY_CACHE_HEADER_IMAGE);
            }
            HeaderImageList result = gson.fromJson(data, HeaderImageList.class);
            if (result != null) {
                int randomIndex = (int) (Math.random() * result.getList().size());
                headerImage = result.getList().get(randomIndex);
            }
        } catch (Exception e) {
            Log.e("test", e.getMessage());
        }
    }

    public static Single<Boolean> getHeaderImageAsync() {
        return Single.create(emitter -> {
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_SHARE_HEADER))
                    .newBuilder()
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = GWApiManager.shared().getClient().newCall(request).execute()) {
                JsonObject jsonObject = GWApiManager.shared().handleResponse(response, null);
                if (jsonObject != null) {
                    JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_HEADER_IMAGE, jsonObject.toString());
                    setHeaderImage(jsonObject.toString());
                    emitter.onSuccess(true);
                } else {
                    setHeaderImage(null);
                    emitter.onSuccess(false);
                }
            } catch (IOException e) {
                setHeaderImage(null);
                emitter.onSuccess(false);
            }
        });
    }

}
