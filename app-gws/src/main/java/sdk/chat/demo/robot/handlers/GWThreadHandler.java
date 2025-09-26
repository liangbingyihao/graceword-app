package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.greenrobot.greendao.query.QueryBuilder;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import sdk.chat.core.base.AbstractThreadHandler;
import sdk.chat.core.dao.CachedFile;
import sdk.chat.core.dao.DaoCore;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.MessageDao;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.ThreadDao;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.rigs.MessageSendRig;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.BuildConfig;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.adpter.data.ArticleSession;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.AIFeedback;
import sdk.chat.demo.robot.api.model.GWConfigs;
import sdk.chat.demo.robot.api.model.ImageDaily;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.api.model.SystemConf;
import sdk.chat.demo.robot.extensions.DateLocalizationUtil;
import sdk.chat.demo.robot.extensions.LanguageUtils;
import sdk.chat.demo.robot.extensions.StateStorage;
import sdk.chat.demo.robot.holder.AIFeedbackType;
import sdk.chat.demo.robot.holder.HolderProvider;
import sdk.chat.demo.robot.holder.MessageHolder;
import sdk.guru.common.RX;

public class GWThreadHandler extends AbstractThreadHandler {
    private final AtomicBoolean hasSyncedWithNetwork = new AtomicBoolean(false);
    private List<ArticleSession> sessionCache;
    private Message welcome;
    private AIExplore aiExplore;
    //    private Message playingMsg;
    private Boolean isCustomPrompt = null;
    private SystemConf serverPrompt = null;
    private final static Gson gson = new Gson();
    public final static String KEY_AI_FEEDBACK = "ai_feedback";
    public final static String headTopic = "信仰问答";
//    public static String qaThreadId = null;

//    public AIExplore getAiExplore() {
//        return aiExplore;
//    }


    public Single<SystemConf> getServerPrompt() {
        if (BuildConfig.DEBUG) {
            Log.d("BuildMode", "当前是 Debug 包");

            if (serverPrompt != null) {
                return Single.just(serverPrompt);
            }

            return GWApiManager.shared().getConf()
                    .subscribeOn(RX.io())
                    .flatMap(conf -> {
                        if (conf == null) {
                            return Single.error(new NullPointerException("Configuration is null"));
                        }
                        serverPrompt = conf;
                        return Single.just(serverPrompt);
                    }).subscribeOn(RX.db());
        } else {
            return Single.just(serverPrompt);
        }
    }

    public boolean isCustomPrompt() {
        if (isCustomPrompt == null) {
            try {
                isCustomPrompt = MainApp.getContext().getSharedPreferences(
                        "ai_prompt",
                        Context.MODE_PRIVATE // 仅当前应用可访问
                ).getBoolean("isCustomPrompt", false);
            } catch (Exception e) {
                isCustomPrompt = false;
            }
        }
        return isCustomPrompt;
    }

    public void setCustomPrompt(boolean customPrompt) {
        MainApp.getContext().getSharedPreferences(
                "ai_prompt",
                Context.MODE_PRIVATE // 仅当前应用可访问
        ).edit().putBoolean("isCustomPrompt", customPrompt).apply();
        isCustomPrompt = customPrompt;
    }

    private List<Message> fetchMessagesEarlier(Long startId) {
        // Logger.debug(java.lang.Thread.currentThread().getName());
        DaoCore daoCore = ChatSDK.db().getDaoCore();


        QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);

        if (startId > 0) {
            qb.where(MessageDao.Properties.Id.lt(startId));
        }

        qb.orderDesc(MessageDao.Properties.Date).limit(20);


        return qb.list();
    }

    public Single<List<Message>> loadMessagesBySession(@Nullable String sessionId) {
        return Single.defer(() -> {
            DaoCore daoCore = ChatSDK.db().getDaoCore();
            QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
            if (sessionId != null) {
                qb.where(MessageDao.Properties.ThreadId.eq(Long.parseLong(sessionId)));
            }
            qb.orderDesc(MessageDao.Properties.Date);
            List<Message> data = qb.list();
            return Single.just(data);
        }).subscribeOn(RX.db());
    }

    public Single<Boolean> setSummary(String msgId, String summary) {
        if (summary == null || summary.isEmpty() || summary.length() > 8) {
            return Single.just(Boolean.FALSE);
        }
        if (msgId == null || msgId.isEmpty()) {
            return Single.error(new IllegalArgumentException("Message ID cannot be null or empty"));
        }

        return GWApiManager.shared().setSummary(msgId, summary)
                .subscribeOn(RX.io())
                .flatMap(isSuccess -> {
                    if (!isSuccess) {
                        return Single.just(false);
                    }
                    return Single.fromCallable(() -> {

                        Message message = ChatSDK.db().fetchMessageWithEntityID(msgId);
                        if (message != null) {
                            JsonObject data = gson.fromJson(message.stringForKey(KEY_AI_FEEDBACK), JsonObject.class);
                            if (data != null) {
                                data.addProperty("summary", summary);
                                updateMessage(message, data);
                            }
                            return true;
                        }
                        return false;
                    }).subscribeOn(RX.db()).map(updateResult -> true);
                });
    }

    public Single<Long> setMsgSession(String msgId, Long sessionId) {
        if (sessionId == null) {
            return Single.error(new IllegalArgumentException("SessionId ID cannot be null or empty"));
        }
        if (msgId == null || msgId.isEmpty()) {
            return Single.error(new IllegalArgumentException("Message ID cannot be null or empty"));
        }

        return GWApiManager.shared().setMsgSession(msgId, sessionId, null)
                .subscribeOn(RX.io())
                .flatMap(newSessionId -> {
                    if (newSessionId == null) {
                        return Single.just(0L);
                    }
                    return Single.fromCallable(() -> {

                        Message message = ChatSDK.db().fetchMessageWithEntityID(msgId);
                        if (message != null) {
                            Long oldSessionId = message.getThreadId();
                            message.setThreadId(newSessionId);
                            ChatSDK.db().update(message, false);
                            updateThreadLastLastMessageDate(newSessionId);
                            updateThreadLastLastMessageDate(oldSessionId);
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                            return newSessionId;
                        }
                        return 0L;
                    }).subscribeOn(RX.db());
                });
    }

    public Single<Long> newMsgSession(String msgId, String sessionName) {
        if (sessionName == null) {
            return Single.error(new IllegalArgumentException("SessionId ID cannot be null or empty"));
        }
        if (msgId == null || msgId.isEmpty()) {
            return Single.error(new IllegalArgumentException("Message ID cannot be null or empty"));
        }

        return GWApiManager.shared().setMsgSession(msgId, 0L, sessionName)
                .subscribeOn(RX.io())
                .flatMap(newSessionId -> {
                    if (newSessionId == null) {
                        return Single.just(0L);
                    }
                    return Single.fromCallable(() -> {
                        Message message = ChatSDK.db().fetchMessageWithEntityID(msgId);
                        if (message != null) {
                            Long oldSessionId = message.getThreadId();
                            message.setThreadId(newSessionId);
                            ChatSDK.db().update(message, false);
                            updateThreadLastLastMessageDate(oldSessionId);
                            if (newSessionId > 0) {
                                updateThread(newSessionId.toString(), sessionName, message.getDate());
                            }
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                            return newSessionId;
                        }
                        return 0L;
                    }).subscribeOn(RX.db()).map(updateResult -> updateResult);
                }).onErrorReturnItem(0L);
    }

    public Single<Boolean> setSessionName(Long sessionId, String sessionName) {
        if (sessionName == null) {
            return Single.error(new IllegalArgumentException("sessionName cannot be null or empty"));
        }
        if (sessionId == null || sessionId <= 0) {
            return Single.error(new IllegalArgumentException("sessionId ID must grater than 0"));
        }

        return GWApiManager.shared().setSessionName(sessionId, sessionName)
                .subscribeOn(RX.io())
                .flatMap(ret -> {
                    if (!ret) {
                        return Single.just(Boolean.FALSE);
                    }
                    return Single.fromCallable(() -> {
                        updateThread(sessionId.toString(), sessionName, null);
                        return Boolean.TRUE;
                    }).subscribeOn(RX.db()).map(updateResult -> updateResult);
                }).onErrorReturnItem(Boolean.FALSE);
    }


    public Single<List<Message>> loadMessagesEarlier(@Nullable Long startId, boolean loadFromServer) {
        return Single.defer(() -> {
            DaoCore daoCore = ChatSDK.db().getDaoCore();
            QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
            if (startId != null && startId > 0) {
                qb.where(MessageDao.Properties.Id.lt(startId));
            }
            qb.orderDesc(MessageDao.Properties.Date).limit(20);
            List<Message> messages = qb.list();
            int i = 0;
            while (aiExplore == null && i < messages.size()) {
                Message tmp = messages.get(i);
                MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(tmp);
                if (aiFeedback != null && aiFeedback.getFeedback() != null) {
                    aiExplore = AIExplore.loads(tmp);
//                    Log.d("onLoadElder", "aiExplore1=" + aiExplore.getMessage().getId());
                }
                if (startId == null || startId == 0) {
                    reloadTimeoutMsg(tmp);
                }
                ++i;
            }
            return Single.just(messages);
        }).subscribeOn(RX.db());
    }

    public Single<List<Message>> loadMessagesLater(@Nullable Long startId, boolean loadFromServer) {
        return Single.defer(() -> {
            DaoCore daoCore = ChatSDK.db().getDaoCore();
            QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
            if (startId != null && startId > 0) {
                qb.where(MessageDao.Properties.Id.gt(startId));
            }
            qb.orderAsc(MessageDao.Properties.Date).limit(20);
            List<Message> messages = qb.list();
//            int i = messages.size() - 1;
//            if (i >= 0) {
//                AIExplore newAiExplore = null;
//                while (newAiExplore == null && i < messages.size()) {
//                    Message tmp = messages.get(i);
//                    MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(tmp);
//                    if (aiFeedback != null && aiFeedback.getFeedback() != null) {
//                        newAiExplore = AIExplore.loads(tmp);
//                    }
//                    --i;
//                }
//                if (newAiExplore != null) {
////                    Message oldMsg = aiExplore != null ? aiExplore.getMessage() : null;
//                    aiExplore = newAiExplore;
//                    Log.d("aiExplore", "aiExplore2=" + aiExplore.getMessage().getId());
////                    if (oldMsg != null) {
////                        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(oldMsg));
////                    }
//                }
//            }
            return Single.just(messages);
        }).subscribeOn(RX.db());
    }
//    @Override
//    public Single<List<Message>> loadMoreMessagesBefore(Thread thread, @Nullable Date before) {
//        return super.loadMoreMessagesBefore(thread, before);
//    }
//
//    @Override
//    public Single<List<Message>> loadMoreMessagesBefore(Thread thread, @Nullable Date before, boolean loadFromServer) {
//        return super.loadMoreMessagesBefore(thread, before, loadFromServer);
//    }

//    @Override
//    public Single<List<Message>> loadMoreMessagesAfter(Thread thread, @Nullable Date after, boolean loadFromServer) {

    /// /        return super.loadMoreMessagesAfter(thread, after, loadFromServer);
//
//        return super.loadMoreMessagesAfter(thread, after, loadFromServer).flatMap(localMessages -> {
//            ArrayList<Message> mergedMessages = new ArrayList<>(localMessages);
//            return Single.just(mergedMessages);
//        });
//    }
    public Completable removeUsersFromThread(final Thread thread, List<User> users) {
        return Completable.complete();
    }

    public Completable pushThread(Thread thread) {
        return Completable.complete();
    }

    @Override
    public boolean muteEnabled(Thread thread) {
        return true;
    }

    public Completable sendExploreMessage(final String text, final Message contextMsg, int action, String params) {
        if (action == AIExplore.ExploreItem.action_bible_pic) {
            return genBiblePic(contextMsg);
        }
        Thread thread = contextMsg != null && contextMsg.getThread() != null ? contextMsg.getThread() : ChatSDK.db().fetchThreadWithEntityID("0");
        return new MessageSendRig(new MessageType(MessageType.Text), thread, message -> {
            message.setText(text);
            if (action == AIExplore.ExploreItem.action_input_prompt) {
                message.setMetaValue("reply", params);
            } else {
                message.setMetaValue("context_id", contextMsg!=null?contextMsg.getEntityID():"empty");
                message.setMetaValue("action", action);
            }
//    action_daily_ai = 0
//    action_bible_pic = 1
//    action_daily_gw = 2
//    action_direct_msg = 3
//    action_daily_pray = 4
            if (action == AIExplore.ExploreItem.action_direct_msg) {
                message.setMetaValue("feedback", params);
            } else if (action == AIExplore.ExploreItem.action_daily_gw || action == AIExplore.ExploreItem.action_daily_gw_pray) {
                message.setType(HolderProvider.GWMessageType);
                message.setMetaValue("image-date", params);
            } else if (action == AIExplore.ExploreItem.action_daily_pray && params != null && !params.isEmpty()) {
                message.setText(params + "\n" + text);
            }
        }).run();
    }

    private Completable genBiblePic(Message message) {
        MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
        if (aiFeedback == null || aiFeedback.getFeedback() == null || aiFeedback.getFeedback().getBible() == null || aiFeedback.getFeedback().getBible().isEmpty()) {
            return Completable.complete();
        }
        return ImageApi.listImageTags().subscribeOn(RX.io()).flatMap(data -> {
            String tag = aiFeedback.getFeedback().getTag();
            String imageUrl = ImageApi.getRandomImageByTag(tag);
            message.setMetaValue(Keys.ImageUrl, imageUrl);
//            message.setType(MessageType.Image);
            ChatSDK.db().update(message, false);
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
            return Single.just(message);
        }).onErrorResumeNext(Single::error).ignoreElement();
    }

    public void reloadTimeoutMsg() {

        if (timeoutMsgRemoteId != null && pendingMsgId == null) {
            Message message = ChatSDK.db().fetchMessageWithEntityID(timeoutMsgRemoteId);
            reloadTimeoutMsg(message);
        }
    }

    private void reloadTimeoutMsg(Message message) {
        try {
            MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
            if (message != null && !message.isLocalMessage() && (aiFeedback == null || aiFeedback.getStatus() <= MessageDetail.STATUS_PENDING)) {
                //可能是上次没取完的数据
                Log.d("sending", "timeout msg content:" + message.getText());
                startPolling(message.getId(), message.getEntityID(), 0);
                Log.d("sending", "startPolling timeout msg");
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Send a text,
     * The text need to have a owner thread attached to it or it cant be added.
     * If the destination thread is public the system will add the user to the text thread if needed.
     * The uploading to the server part can bee seen her {@see FirebaseCoreAdapter#PushMessageWithComplition}.
     */
    public Completable sendMessage(final Message message) {

        if (!message.isLocalMessage()) {
            //重试查看AI回复
            try {
//                JsonObject data = gson.fromJson(message.stringForKey(KEY_AI_FEEDBACK), JsonObject.class);
//                if(data==null){
//                    data = new JsonObject();
//                }
//                if (data.has("status")) {
//                    data.addProperty("status", MessageDetail.STATUS_PENDING);
//                }
//                updateMessage(message, data);
                startPolling(message.getId(), message.getEntityID(), 1);
                return Completable.complete();
            } catch (Exception e) {
                return Completable.error(e);
            }
        }

        Integer action = message.integerForKey("action");
        if (action != null) {
            if (action == AIExplore.ExploreItem.action_direct_msg) {
                return Completable.complete();
            } else if (action == AIExplore.ExploreItem.action_daily_gw || action == AIExplore.ExploreItem.action_daily_gw_pray) {
                String imageDate = message.stringForKey("image-date");
                return ImageApi.listImageDaily("").subscribeOn(RX.io()).flatMap(data -> {
                    if (data != null && !data.isEmpty()) {
                        ImageDaily m = null;
                        if (imageDate != null && !imageDate.isEmpty()) {
                            for (ImageDaily d : data) {
                                if (d.getDate().equals(imageDate)) {
                                    m = d;
                                    break;
                                }
                            }
                        }
                        if (m == null) {
                            m = data.get(0);
                        }
                        message.setMetaValue(Keys.ImageUrl, m.getUrl());
                        MessageDetail messageDetail = new MessageDetail();
                        messageDetail.setStatus(2);
                        AIFeedback aiFeedback = new AIFeedback();
                        messageDetail.setFeedback(aiFeedback);
                        aiFeedback.setFunctions(m.getExploreWithParams());
                        if (!aiFeedback.getFunctions().isEmpty()) {
                            updateMessage(message, gson.toJsonTree(messageDetail).getAsJsonObject());
                        }

                        if (action == AIExplore.ExploreItem.action_daily_gw_pray) {
                            // 处理数据
                            Observable.just(data)
                                    .delay(1, TimeUnit.SECONDS) // 延迟2秒
                                    .ignoreElements() // 转换为Completable
                                    .andThen(sendExploreMessage(
                                            "",
                                            message,
                                            AIExplore.ExploreItem.action_daily_pray,
                                            m.getScripture()
                                    )).subscribe(
                                    );
//                            sendExploreMessage(
//                                    "关于以上内容的祷告和默想建议",
//                                    message,
//                                    action_daily_pray,
//                                    m.getScripture()
//                            ).subscribe();
                        }
                    }
                    return Single.just(message);
                }).onErrorResumeNext(Single::error).ignoreElement();
            }
        }

        if (pendingMsgId != null && pendingMsgId > 0) {
            return Completable.error(new Exception(MainApp.getContext().getString(R.string.sending)));
        }


        if (action != null && action == AIExplore.ExploreItem.action_daily_pray) {
            DailyTaskHandler.completeTaskByIndex(1);
        } else if (action == null || action == 0) {
            String contextId = message.stringForKey("context_id");
            if (contextId == null || contextId.isEmpty()) {
                DailyTaskHandler.completeTaskByIndex(2);
            }
        }

        String prompt = null;
        if (isCustomPrompt) {
            String k = "ai_prompt";
            if (message.getMetaValuesAsMap().containsKey("action")) {
                k = "ai_prompt" + action;
            }
            prompt = MainApp.getContext().getSharedPreferences(
                    "ai_prompt",
                    Context.MODE_PRIVATE // 仅当前应用可访问
            ).getString(k, null);
        }

        message.setMessageStatus(MessageSendStatus.Uploading, true);
        pendingMsgId = message.getId();
        aiExplore = null;
        return GWApiManager.shared().askRobot(message, prompt)
                .subscribeOn(RX.io()).flatMap(data -> {
                    String entityId = data.get("id").getAsString();
                    message.setEntityID(entityId);
                    message.setMessageStatus(MessageSendStatus.Replying, false);
                    ChatSDK.db().getDaoCore().getDaoSession().update(message);
//                    ChatSDK.db().update(message);
                    startPolling(message.getId(), entityId, 0);
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    pushForMessage(message);
//                    ChatSDK.events().source().accept(NetworkEvent.threadMessagesUpdated(message.getThread()));
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    ChatSDK.events().source().accept(NetworkEvent.messageSendStatusChanged(message));
                    return Single.just(message);
                })
                .doOnError(throwable -> {
                    pendingMsgId = null;
                    ChatSDK.events().source().accept(NetworkEvent.messageSendStatusChanged(message));
                }).onErrorResumeNext(Single::error)
                .ignoreElement();

    }

    @SuppressLint("CheckResult")
    public Single<Thread> createThread(String name, List<User> theUsers, int type, String entityID, String imageURL, Map<String, Object> meta) {

        if (entityID != null) {
            Thread t = ChatSDK.db().fetchThreadWithEntityID(entityID);
            if (t != null) {
                return Single.just(t);
            }
        }

        return GWApiManager.shared().saveSession("1")
                .subscribeOn(RX.io()).flatMap(data -> {
                    final Thread thread = ChatSDK.db().createEntity(Thread.class);
                    thread.setEntityID(data.get("id").getAsString());
                    thread.setCreator(ChatSDK.currentUser());
                    thread.setCreationDate(new Date());
                    if (name != null) {
                        thread.setName(name, false);
                    }

                    if (imageURL != null) {
                        thread.setImageUrl(imageURL, false);
                    }

                    ArrayList<User> users = new ArrayList<>();
                    if (theUsers != null && !theUsers.isEmpty()) {
                        users.addAll(theUsers);
                    }
                    User currentUser = ChatSDK.currentUser();
                    users.add(currentUser);
                    thread.addUsers(users);
                    thread.setType(ThreadType.Private1to1);
                    ChatSDK.db().update(thread);
                    return Single.just(thread);
                });
    }

    public Single<Boolean> clearFeedbackText(Message message) {
        return GWApiManager.shared().clearMsg(message.getEntityID(), GWApiManager.contentTypeAI)
                .flatMap(result -> {
                    if (!result) {
                        return Single.error(new IllegalStateException("Invalid API response: " + result));
                    }
                    return Single.fromCallable(() -> {
                        Integer action = message.integerForKey("action");
                        if (message.getText().isEmpty() || (action != null && action > 0)) {
                            message.cascadeDelete();
                            ChatSDK.events().source().accept(NetworkEvent.messageRemoved(message));
                        } else {
                            JsonObject data = gson.fromJson(message.stringForKey(KEY_AI_FEEDBACK), JsonObject.class);
                            if (data != null && data.has("feedback_text")) {
                                data.addProperty("feedback_text", "");
                                if(data.has("feedback")){
                                    data.remove("feedback");
                                }
                                message.setMetaValue(KEY_AI_FEEDBACK, data.toString());
//                                ChatSDK.db().update(message, false);
                                ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
//                                updateMessage(message, data);
                            }
                        }

                        return result;
                    }).subscribeOn(RX.db());
                }).onErrorResumeNext(Single::error);
    }


    public Single<Boolean> clearUserText(Message message) {
        return GWApiManager.shared().clearMsg(message.getEntityID(), GWApiManager.contentTypeUser)
                .flatMap(result -> {
                    if (!result) {
                        return Single.error(new IllegalStateException("Invalid API response"));
                    }
                    return Single.fromCallable(() -> {

                        MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
                        if (message.getMessageStatus() != MessageSendStatus.Sent || aiFeedback == null || aiFeedback.getFeedbackText().isEmpty()) {
                            message.cascadeDelete();
                            ChatSDK.events().source().accept(NetworkEvent.messageRemoved(message));
                        } else {
                            message.setText("");
                            ChatSDK.db().update(message, false);
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                        }

                        return result;
                    }).subscribeOn(RX.db());
                }).onErrorResumeNext(Single::error);
    }

    public Single<Integer> toggleAiLikeState(Message message) {
        // 其他错误传递到UI层
        return GWApiManager.shared().toggleFavorite(message.getEntityID(), GWApiManager.contentTypeAI)
                .flatMap(result -> {
                    if (result != 0 && result != 1) {
                        return Single.error(new IllegalStateException("Invalid API response: " + result));
                    }

                    return Single.fromCallable(() -> {
                        int newState = StateStorage.setStateB(message.getStatus(), result == 1);
                        message.setStatus(newState);
                        ChatSDK.db().update(message, false);
                        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                        return result;
                    }).subscribeOn(RX.db());
                }).onErrorResumeNext(Single::error);
    }

    public Single<Integer> toggleContentLikeState(Message message) {
//        message.setStatus(StateStorage.toggleStateA(message.getStatus()));
//        ChatSDK.db().update(message, false);
//        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));

        // 其他错误传递到UI层
        return GWApiManager.shared().toggleFavorite(message.getEntityID(), GWApiManager.contentTypeUser)
                .flatMap(result -> {
                    if (result != 0 && result != 1) {
                        return Single.error(new IllegalStateException("Invalid API response: " + result));
                    }

                    return Single.fromCallable(() -> {
                        int newState = StateStorage.setStateA(message.getStatus(), result == 1);
                        message.setStatus(newState);
                        ChatSDK.db().update(message, false);
                        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                        return result;
                    }).subscribeOn(RX.db());
                }).onErrorResumeNext(Single::error);
    }

    public Single<Boolean> renew(Message message) {
        Integer action = message.integerForKey("action");
        String prompt = null;
        if (isCustomPrompt) {
            String k = "ai_prompt";
            if (message.getMetaValuesAsMap().containsKey("action")) {
                k = "ai_prompt" + action;
            }
            prompt = MainApp.getContext().getSharedPreferences(
                    "ai_prompt",
                    Context.MODE_PRIVATE // 仅当前应用可访问
            ).getString(k, null);
        }

        // 其他错误传递到UI层
        return GWApiManager.shared().renew(message.getEntityID(), prompt)
                .subscribeOn(RX.io()).flatMap(data -> {
                    startPolling(message.getId(), message.getEntityID(), 0);
                    return Single.just(data);
                }).onErrorResumeNext(Single::error);
    }

    public Single<Boolean> deleteSession(String sessionId) {
        return GWApiManager.shared().deleteSession(sessionId)
                .flatMap(result -> {
                    if (!result) {
                        return Single.error(new IllegalStateException("删除失败，稍后重试"));
                    }

                    return Single.fromCallable(() -> {

                        DaoCore daoCore = ChatSDK.db().getDaoCore();

                        String sql = "UPDATE MESSAGE SET THREAD_ID = -1 " +
                                "WHERE THREAD_ID =" + sessionId;
                        daoCore.getDaoSession().getDatabase().execSQL(sql);


                        Thread thread = ChatSDK.db().fetchThreadWithEntityID(sessionId);
                        ChatSDK.db().delete(thread);
                        sessionCache = null;
                        ChatSDK.events().source().accept(NetworkEvent.threadsUpdated(Long.parseLong(sessionId)));
                        return result;
                    }).subscribeOn(RX.db());
                }).onErrorResumeNext(Single::error);
    }

    protected void pushForMessage(final Message message) {
//        if (ChatSDK.push() != null && message.getThread().typeIs(ThreadType.Private)) {
//            Map<String, Object> data = ChatSDK.push().pushDataForMessage(message);
//            ChatSDK.push().sendPushNotification(data);
//        }
    }

    public Completable deleteMessage(Message message) {
        return Completable.defer(() -> {
            if (message.getSender().isMe() && message.getMessageStatus().equals(MessageSendStatus.Sent) && !message.getMessageType().is(MessageType.System)) {
                // If possible delete the files associated with this message

                List<CachedFile> files = ChatSDK.db().fetchFilesWithIdentifier(message.getEntityID());
                for (CachedFile file : files) {
                    if (file.getRemotePath() != null) {
                        ChatSDK.upload().deleteFile(file.getRemotePath()).subscribe();
                    }
                    ChatSDK.db().delete(file);
                }

                // TODO: 删除后端...
//                MessagePayload payload = ChatSDK.getMessagePayload(message);
//                if (payload != null) {
//                    List<String> paths = payload.remoteURLs();
//                    for (String path: paths) {
//                        ChatSDK.upload().deleteFile(path).subscribe();
//                    }
//                }

//                return FirebaseModule.config().provider.messageWrapper(message).delete();
            }
            message.getThread().removeMessage(message);
            return Completable.complete();
        });
    }

    public Completable leaveThread(Thread thread) {
        //FIXME
        return Completable.complete();
    }

    public Completable joinThread(Thread thread) {
        return null;
    }

    @Override
    public boolean canJoinThread(Thread thread) {
        return false;
    }

    @Override
    public boolean rolesEnabled(Thread thread) {
        return thread.typeIs(ThreadType.Group);
    }


    @Override
    public String localizeRole(String role) {
        return "";
    }


    @Override
    public String generateNewMessageID(Thread thread) {
        // User Firebase to generate an ID
        return Long.toString(System.currentTimeMillis());
    }

    @Override
    public boolean canDestroy(Thread thread) {
        return false;
    }

    @Override
    public Completable destroy(Thread thread) {
        return Completable.complete();
    }


    public void clearThreadCache() {
        sessionCache = null;
        ChatSDK.events().source().accept(NetworkEvent.threadsUpdated(0L));
    }

    public String getSessionName(Long sessionId) {
        if (sessionId != null && sessionId > 0 && sessionCache != null && !sessionCache.isEmpty()) {
            String strId = sessionId.toString();
            for (ArticleSession session : sessionCache) {
                if (session.getId().equals(strId)) {
                    return session.getTitle();
                }

            }
        }
        return null;
    }

    public Single<List<ArticleSession>> listSessions() {
        // 1. 检查内存缓存
        if (sessionCache != null && !sessionCache.isEmpty()) {
            List<ArticleSession> ret = new ArrayList<>(sessionCache);
            return Single.just(ret);
        }
        return Single.fromCallable(() -> {
            DaoCore daoCore = ChatSDK.db().getDaoCore();
            QueryBuilder<Thread> qb = daoCore.getDaoSession().queryBuilder(Thread.class);
            qb.whereOr(
                    ThreadDao.Properties.Type.eq(ThreadType.None),
                    ThreadDao.Properties.Type.eq(ThreadType.PublicGroup)
            );
            qb.orderDesc(ThreadDao.Properties.Type).orderDesc(ThreadDao.Properties.LastMessageDate);
            List<Thread> threads = qb.list();

            List<ArticleSession> ret = new ArrayList<>();
            // 新增逻辑：将名为"信仰问答"的线程提到第一位
//            Log.e("listSessions", "sort...."+ AppCompatDelegate.getApplicationLocales());
            if (threads != null && !threads.isEmpty()) {
                // 查找名为headTopic的线程
                for (Thread thread : threads) {
                    ArticleSession s;
                    if (headTopic.equals(thread.getName())) {
                        s = new ArticleSession(thread.getEntityID(), Objects.requireNonNull(LanguageUtils.INSTANCE.getString(R.string.questions)), true);
                    } else {
                        s = new ArticleSession(thread.getEntityID(), thread.getName(), false);
                    }
                    ret.add(s);
                    Log.e("listSessions", s.isQA() + "," + s.getTitle() + "," + thread.getType());
                }
//                    for (int i = 0; i < threads.size(); i++) {
//                        if (headTopic.equals(threads.get(i).getName())) {
//                            testThread = threads.remove(i); // 从原位置移除
//                            break;
//                        }
//                    }

//                    // 如果找到headTopic线程，则插入到第一位
//                    if (testThread != null) {
//                        threads.add(0, testThread); // 添加到列表开头
//                    }
            }

//                List<Thread> data = ChatSDK.db().fetchThreadsWithType(ThreadType.None);
//                Collections.sort(data, (a, b) -> {
//                    return Long.compare(b.getEntityID(), a.getCreationDate().getTime()); // 倒序
//                });

            sessionCache = new ArrayList<>(ret);
            if (!hasSyncedWithNetwork.get()) {
                triggerNetworkSync();
            }
            return ret;
        }).subscribeOn(RX.io());
    }

//    public Single<List<Thread>> newAndListSessions() {
//        //                    if (!sessions.isEmpty() && sessions.get(0).getMessages().isEmpty()) {
//        //                        return Single.just(sessions);
//        //                    }
//        //                    // 如果不存在会话，则创建新会话后再查询
//        //                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
//        //                            .flatMap(ignored -> {
//        //                                sessionCache = null;
//        //                                return listSessions();
//        //                            });
//        return listSessions()
//                .flatMap(Single::just)
//                .onErrorResumeNext(error -> {
//                    // 错误处理逻辑
//                    if (error instanceof IOException) {
//                        return Single.error(new IOException("Failed to list sessions", error));
//                    }
//                    return Single.error(error);
//                });
//    }

    @SuppressLint("CheckResult")
    public void triggerNetworkSync() {
        GWApiManager.shared().listSession(1, 100)
                .subscribeOn(RX.io())
//                .observeOn(RX.io())
                .subscribe(
                        networkSessions -> {
                            boolean modified = false;
                            if (networkSessions != null) {
                                JsonArray items = networkSessions.getAsJsonObject().getAsJsonArray("items");
                                if (!items.isEmpty()) {
                                    for (JsonElement i : items) {
                                        JsonObject session = i.getAsJsonObject();
                                        String sessionName = session.get("session_name").getAsString();
                                        String entityId = session.get("id").getAsString();
                                        Date updateAt = DateLocalizationUtil.INSTANCE.toDate(session.get("updated_at").getAsString());
                                        if (updateThread(entityId, sessionName, updateAt)) {
                                            modified = true;
                                        }
                                    }
                                }
                            }
                            hasSyncedWithNetwork.set(true);

                            // 更新内存缓存
                            if (modified) {
                                sessionCache = null;
                                ChatSDK.events().source().accept(NetworkEvent.threadsUpdated(0L));
                            }
                        },
                        error -> {
                        }
                );
    }

    public final static String chatSessionId = "0";

    public Thread createChatSessions() {
        //FIXME
        DaoCore daoCore = ChatSDK.db().getDaoCore();
        QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
        qb.where(MessageDao.Properties.Type.eq(MessageType.Text));
        List<Message> data = qb.list();
        long notTime = new Date().getTime();
        int expireMs = 2 * 60 * 1000;
        for (Message d : data) {
            MessageSendStatus status = d.getMessageStatus();
            if (status != MessageSendStatus.Sent) {
                if (d.isLocalMessage()) {
                    d.setMessageStatus(MessageSendStatus.UploadFailed, false);
                } else {
                    MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(d);
                    if (aiFeedback != null && aiFeedback.getStatus() == MessageDetail.STATUS_SUCCESS) {
                        d.setMessageStatus(MessageSendStatus.Sent, false);
                    } else {
                        d.setMessageStatus(MessageSendStatus.Failed, false);
                        //TODO isExpired
//                        boolean isExpired = d.getDate() == null || (notTime - d.getDate().getTime() > expireMs);
                    }
                }
            }
        }

        Thread thread = ChatSDK.db().fetchThreadWithEntityID(chatSessionId);
        if (thread != null) {
            return thread;
        }
        thread = ChatSDK.db().createEntity(Thread.class);
        thread.setEntityID(chatSessionId);
        thread.setCreator(ChatSDK.currentUser());
        thread.setCreationDate(new Date());
        thread.setName("chat", false);
        ArrayList<User> users = new ArrayList<>(ChatSDK.contact().contacts());
        User currentUser = ChatSDK.currentUser();
        users.add(currentUser);
        thread.addUsers(users);
        thread.setType(ThreadType.Private1to1);
        ChatSDK.db().update(thread);
        return thread;
    }

    public void updateThreadLastLastMessageDate(Long threadId) {
        DaoCore daoCore = ChatSDK.db().getDaoCore();
        QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
        qb.where(MessageDao.Properties.ThreadId.eq(threadId))
                .orderDesc(MessageDao.Properties.Date)  // 关键修改：按时间降序
                .limit(1);                               // 只取第一条
        Message data = qb.unique();

        Thread entity = ChatSDK.db().fetchOrCreateThreadWithEntityID(threadId.toString());
        if (data != null) {
            entity.setLastMessageDate(data.getDate());
        } else {
            entity.setLastMessageDate(new Date(1640995200000L));//给一个很小的时间，使得可以靠后
        }
        ChatSDK.db().update(entity);
        sessionCache = null;
        ChatSDK.events().source().accept(NetworkEvent.threadsUpdated(threadId));
    }

    public boolean updateThread(String threadId, String sessionName, Date updateAt) {
        Thread entity = ChatSDK.db().fetchOrCreateThreadWithEntityID(threadId);
        boolean modified = false;
        if (sessionName != null && !sessionName.isEmpty() && !sessionName.equals(entity.getName())) {
            entity.setName(sessionName);
//            entity.setType(ThreadType.None);
            modified = true;
        }
        if (headTopic.equals(sessionName)) {
            if (entity.getType() == null || entity.getType() != ThreadType.PublicGroup) {
                entity.setType(ThreadType.PublicGroup);
                Log.e("listSessions", sessionName + ",setType:" + ThreadType.PublicGroup);
                modified = true;
            }
        } else if (entity.getType() == null || entity.getType() != ThreadType.None) {
            Log.e("listSessions", sessionName + ",setType:" + ThreadType.None);
            entity.setType(ThreadType.None);
            modified = true;
        }
        if (updateAt != null) {
            entity.setLastMessageDate(updateAt);
            modified = true;
        }
        if (modified) {
            ChatSDK.db().update(entity);
            sessionCache = null;
            ChatSDK.events().source().accept(NetworkEvent.threadsUpdated(Long.parseLong(threadId)));
        }
        return modified;
    }


    public Message newMessage(int type, Thread thread, boolean notify) {
        Message message = new Message();
        message.setSender(ChatSDK.currentUser());
        message.setDate(new Date());

        message.setEntityID(generateNewMessageID(thread));
        message.setType(type);
        message.setMessageStatus(MessageSendStatus.Initial, false);
        message.setIsRead(true);

        ChatSDK.db().insertOrReplaceEntity(message);

        return message;
    }


    @SuppressLint("CheckResult")
    public Single<Message> getWelcomeMsg() {
        if (welcome != null) {
            return Single.just(welcome);
        }
        return Single.fromCallable(() -> {
            try {
                DaoCore daoCore = ChatSDK.db().getDaoCore();
                QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
                qb.where(MessageDao.Properties.EntityID.eq("welcome")).limit(1);
                List<Message> data = qb.list();

                if (data.isEmpty()) {
                    ImageApi.getServerConfigs()
                            .subscribeOn(RX.io())
                            .subscribe(
                                    json -> {
                                    },
                                    error -> {
                                    }
                            );
                }
                welcome = data.get(0);
                return welcome;
            } catch (Exception e) {
                throw new IOException("Failed to get threads", e);
            }
        }).subscribeOn(RX.io());
//        return listSessions()
//                .flatMap(Single::just)
//                .onErrorResumeNext(error -> {
//                    // 错误处理逻辑
//                    if (error instanceof IOException) {
//                        return Single.error(new IOException("Failed to list sessions", error));
//                    }
//                    return Single.error(error);
//                });
    }


    public void updateMessage(Message message, JsonObject json) {
        if (json == null || message == null) {
            return;
        }
//        Log.d("sending", "updateMessage:" + json.toString());
        try {
            MessageDetail aiFeedback = gson.fromJson(json, MessageDetail.class);
            if (aiFeedback == null) {
                return;
            }
            message.setMetaValue(KEY_AI_FEEDBACK, json.toString());

            if (aiFeedback.getStatus() > MessageDetail.STATUS_PENDING) {
                Log.d("sending", "aiFeedback.getStatus()=" + aiFeedback.getStatus());
                disposables.clear();
            }

            Long sid = aiFeedback.getSessionId();
            if (sid != null && sid > 0 && !sid.equals(message.getThreadId())) {
                message.setThreadId(sid);
                ChatSDK.db().update(message);
                if (aiFeedback.getFeedback() != null) {
                    updateThread(Long.toString(sid), aiFeedback.getFeedback().getTopic(), new Date());
                }
            }

            if (aiFeedback.getStatus() > MessageDetail.STATUS_SUCCESS) {
                AIFeedback fb = aiFeedback.getFeedback();
                if (aiFeedback.getStatus() == MessageDetail.STATUS_ERROR) {
                    GWConfigs configs = ImageApi.getGwConfigs();
                    if (configs != null) {
                        AIFeedback feedback = configs.getDefaultMsg().getFeedback();
                        if (feedback != null) {
                            aiFeedback.setFeedback(feedback);
                            aiFeedback.setFeedbackText(feedback.getView());
                            message.setMetaValue(KEY_AI_FEEDBACK, gson.toJson(aiFeedback));
                        }
                    }
                    message.setMessageStatus(MessageSendStatus.Sent, false);
                } else if (aiFeedback.getStatus() == MessageDetail.STATUS_CANCEL || (fb != null && fb.getView() != null && !fb.getView().isEmpty())) {
                    message.setMessageStatus(MessageSendStatus.Sent, false);
                } else {
                    message.setMessageStatus(MessageSendStatus.Failed, false);
                }
            }

            if (aiFeedback.getStatus() == MessageDetail.STATUS_SUCCESS) {
                message.setMessageStatus(MessageSendStatus.Sent, false);
                if (sid != null && sid > 0 && !sid.equals(message.getThreadId())) {
                    message.setThreadId(sid);
                    if (aiFeedback.getFeedback() != null) {
                        updateThread(Long.toString(sid), aiFeedback.getFeedback().getTopic(), new Date());
                    }
                }

                if (aiFeedback.getFeedback() != null && (aiExplore == null || aiExplore.getMessage().getId() <= message.getId())) {
                    AIExplore newAIExplore = AIExplore.loads(message);
                    if (newAIExplore != null) {
                        aiExplore = newAIExplore;
                    }
                }
            }

            MessageHolder holder = HolderProvider.INSTANCE.getExitsMessageHolder(message);
            if (holder instanceof AIFeedbackType) {
                AIFeedbackType aiHolder = (AIFeedbackType) holder;
                //需要刷新
                aiHolder.setAiFeedback(aiFeedback);
            }

            Log.d("sending", "send event");
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
        } catch (Exception e) {
            Log.d("sending", "update Exception =" + e.getMessage());
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
//            Logger.warn(e.getMessage());
        }

    }

    public Message getWelcome() {
        return welcome;
    }

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final AtomicInteger retryCount = new AtomicInteger(0);    // 配置参数
    private volatile Long pendingMsgId = null;
    private volatile String timeoutMsgRemoteId = null;
    private static final long INITIAL_DELAY = 0; // 立即开始
    private static final long POLL_INTERVAL = 2; // 5秒间隔
    private static final long REQUEST_TIMEOUT = 15; // 单个请求10秒超时
    private static final long OPERATION_TIMEOUT = 60; // 整体操作1分钟超时
    private static final int MAX_RETRIES = 3; // 最大重试次数

    public synchronized Long pendingMsgId() {
        return pendingMsgId;
    }

    public synchronized void stopPolling() {
        retryCount.set(0);
    }

    public synchronized void startPolling(Long localId, String contextId, int retry) throws Exception {
        if (pendingMsgId != null && !pendingMsgId.equals(localId)) {
//            disposables.clear();
            throw new Exception(LanguageUtils.INSTANCE.getString(R.string.sending));
        }

        pendingMsgId = localId;
        retryCount.set(1);
        long timeout = OPERATION_TIMEOUT;
        {
            Log.d("sending", "startPolling:" + localId.toString());
            Message message = ChatSDK.db().fetchMessageWithEntityID(contextId);
            if (message != null && message.getMessageStatus() != MessageSendStatus.Replying) {
                message.setMessageStatus(MessageSendStatus.Replying, true);
                long diffTime = System.currentTimeMillis() - message.getDate().getTime();
                Log.d("sending", "diffTime:" + Long.toString(diffTime) + ",in five minuts:" + (diffTime > 300000 ? "n" : "y"));
                if (diffTime > 300000) {
                    timeout = 12;
                }
            }
        }
        timeoutMsgRemoteId = null;
        Disposable disposable = Observable.interval(INITIAL_DELAY, POLL_INTERVAL, TimeUnit.SECONDS)
                .doOnDispose(() -> {
                            Log.d("sending", "doOnDispose:" + localId.toString());
                            pendingMsgId = null;
                        }
                )
                .flatMap(tick -> {
                    int stop = retryCount.get();
                    if (stop == 0) {
                        retryCount.decrementAndGet();
                    }
                    if (stop < 0) {
                        Log.d("sending", "stop < 0.." + localId.toString());
                        disposables.clear();
//                        return Observable.empty();
                    }
//                    Log.d("sending", stop+",getMessageDetail.." + localId.toString());
                    return GWApiManager.shared().getMessageDetail(contextId, tick == 0 ? retry : 0, stop == 0 ? 1 : 0)
                            .subscribeOn(RX.io())
                            .flatMap(Single::just).toObservable();
                })
                .observeOn(RX.db())
                .flatMapCompletable(json ->
                        {
                            if (json != null) {
                                Message message = ChatSDK.db().fetchMessageWithEntityID(contextId);
                                updateMessage(message, json);
                                return Completable.complete();
                            } else {
                                return Completable.error(new Throwable());
                            }
                        }
                )
                .timeout(timeout, TimeUnit.SECONDS)
                .observeOn(RX.main())
                .subscribe(() -> {
                            Log.d("sending", "success:" + localId.toString());
                        },
                        error -> {
//                            Log.d("sending", "error:" + localId.toString() + "," + error.getMessage());
                            Message message = ChatSDK.db().fetchMessageWithEntityID(contextId);
                            if (message != null) {
                                if (error instanceof TimeoutException) {
                                    timeoutMsgRemoteId = contextId;
                                    Logger.error("get ai feedback timeout: " + message.getEntityID(), error);
                                } else {
                                    Logger.error("get ai feedback error " + message.getEntityID(), error);
                                }
                                message.setMessageStatus(MessageSendStatus.Failed, true);
                                ChatSDK.events().source().accept(NetworkEvent.errorEvent(message, "message", error.getMessage()));
                            }
                        });

        disposables.add(disposable);
    }
}
