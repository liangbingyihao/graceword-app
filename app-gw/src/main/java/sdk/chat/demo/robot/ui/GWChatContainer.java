package sdk.chat.demo.robot.ui;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import sdk.chat.demo.robot.handlers.OffscreenScreenshotHelper;
import sdk.chat.demo.robot.handlers.OffscreenScreenshotHelper.ButtonConfig;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.events.EventType;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.PermissionRequestHandler;
import sdk.chat.core.utils.TimeLog;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.activities.WebViewActivity;
import sdk.chat.demo.robot.adpter.ChatAdapter;
import sdk.chat.demo.robot.api.model.KeyValuePair;
import sdk.chat.demo.robot.api.model.ShareRequest;
import sdk.chat.demo.robot.api.model.ShareRequestKt;
import sdk.chat.demo.robot.api.model.Song;
import sdk.chat.demo.robot.extensions.ImageSaveUtils;
import sdk.chat.demo.robot.handlers.CardGenerator;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.handlers.LogUploader;
import sdk.chat.demo.robot.handlers.SocialShareHandler;
import sdk.chat.demo.robot.holder.HolderProvider;
import sdk.chat.demo.robot.holder.MessageHolder;
import sdk.chat.demo.robot.holder.TextHolder;
import sdk.chat.demo.robot.utils.SocialShareUtils;
import sdk.chat.demo.robot.utils.TemplateUtils;
import sdk.chat.demo.robot.utils.ToastHelper;
import sdk.guru.common.DisposableMap;
import sdk.guru.common.RX;

public class GWChatContainer extends FrameLayout implements MessagesListAdapter.OnLoadMoreListener {

    protected RecyclerView messagesList;
    private LoadMoreSwipeRefreshLayout swipeRefreshLayout;
    protected FrameLayout root;
    //    private View shareMenu;
//    private TextView tvSelected;
    protected boolean listenersAdded = false;
    private long latestMsgId = 0;


    public interface Delegate {
        Thread getThread();

        void onClick(Message message);

        void onLongClick(Message message);

        String getMessageId();

        void onLoadLatestActive();

        void onSocialShare(boolean active, int total);
    }

    protected ChatAdapter messagesListAdapter;

    protected List<MessageHolder> messageHolders = new ArrayList<>();

    protected DisposableMap dm = new DisposableMap();


    protected Delegate delegate;
//    protected boolean loadMoreEnabled = true;

    public GWChatContainer(Context context) {
        super(context);
    }

    public GWChatContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GWChatContainer(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    private final ChatAdapter.OnMessageViewClickListener onSocialShare = new ChatAdapter.OnMessageViewClickListener() {

        @Override
        public void onMessageViewClick(@org.jetbrains.annotations.Nullable View view, @org.jetbrains.annotations.Nullable IMessage message) {
            if (view != null && message != null && message.getClass() == TextHolder.class) {
                Log.e("onSocialShare", "onSocialShare");
                int id = view.getId();
                TextHolder holder = (TextHolder) message;
                if (id == R.id.btn_share_text) {
                    holder.setAiSelected(true);
                    messagesListAdapter.setMultiSelectMode(true);

                    LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPositionWithOffset(holder.getPos(), 200);
                    }

                    LogUploader.reportEvent(
                            "mod_share", List.of(
                                    new KeyValuePair("share_action", "0")
                            ));
                } else if (id == R.id.btn_share_user_text) {
                    holder.setUserSelected(true);
                    messagesListAdapter.setMultiSelectMode(true);

                    LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPositionWithOffset(holder.getPos(), 200);
                    }

                    LogUploader.reportEvent(
                            "mod_share", List.of(
                                    new KeyValuePair("share_action", "0")
                            ));
                } else if (id == R.id.ai_text_container || id == R.id.cb_ai_text) {
                    holder.setAiSelected(!holder.isAiSelected());
                    ((CheckBox) view.findViewById(R.id.cb_ai_text)).setChecked(holder.isAiSelected());
                } else if (id == R.id.user_text_container || id == R.id.cb_user_text) {
                    holder.setUserSelected(!holder.isUserSelected());
                    ((CheckBox) view.findViewById(R.id.cb_user_text)).setChecked(holder.isUserSelected());
                } else if (id == R.id.ai_song_container || id == R.id.cb_ai_song) {
                    holder.setUserSelected(!holder.isUserSelected());
                    ((CheckBox) view.findViewById(R.id.cb_ai_song)).setChecked(holder.isUserSelected());
                } else {
                    return;
                }
//                if (id == R.id.btn_share_text || id == R.id.btn_share_user_text) {
//                    messagesListAdapter.setMultiSelectMode(true);
//                }
                delegate.onSocialShare(messagesListAdapter.isMultiSelectMode(), messagesListAdapter.getCntSelected());
            }
//            shareMenu.setVisibility(View.VISIBLE);
//            tvSelected.setText("已选中1");
        }
    };


    public void initViews() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_gwchat, this);
//

        messagesList = findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        messagesList.setLayoutManager(layoutManager);

        messagesListAdapter = new ChatAdapter();
        messagesListAdapter.registerViewClickListener(R.id.btn_share_text, onSocialShare);
        messagesListAdapter.registerViewClickListener(R.id.btn_share_user_text, onSocialShare);
        messagesListAdapter.registerViewClickListener(R.id.ai_text_container, onSocialShare);
        messagesListAdapter.registerViewClickListener(R.id.user_text_container, onSocialShare);
        messagesListAdapter.registerViewClickListener(R.id.cb_ai_text, onSocialShare);
        messagesListAdapter.registerViewClickListener(R.id.cb_user_text, onSocialShare);
//        messagesListAdapter.registerViewClickListener(R.id.ai_song_container, onSocialShare);
//        messagesListAdapter.registerViewClickListener(R.id.cb_ai_song, onSocialShare);

        messagesList.setAdapter(messagesListAdapter);
        setupRefreshLayout();

        onLoadMore(0, 0);

    }

    public void handleSocialShare(int vid) {
        if (vid == R.id.btConfirm) {
//            Uri imageUri = SocialShareUtils.getDrawableUri(this.getContext(), R.mipmap.ic_launcher);
//            SocialShareUtils.shareHtmlLinkWithPreview(this.getContext(),"testtitle","htmlContent","plainText",imageUri);

            List<TextHolder> selectedItems = messagesListAdapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                ToastHelper.show(getContext(), "Nothing selected...");
            } else {
                LogUploader.reportEvent(
                        "mod_share", List.of(
                                new KeyValuePair("share_msg_count", Integer.toString(selectedItems.size())),
                                new KeyValuePair("share_action", "10")
                        ));
                String summary = selectedItems.get(0).getShareSummary(getContext());
                dm.add(SocialShareHandler.batchShare(ShareRequestKt.createShareRequest(selectedItems))
                        .subscribe(
                                shareUrl -> {
                                    // 成功回调（在主线程）
                                    SocialShareUtils.showCustomShareDialog(getContext(), SocialShareUtils.targetApps, summary, null, shareUrl);
                                },
                                error -> {
                                    // 错误回调（在主线程）
                                    ToastHelper.show(getContext(), error.getMessage());
                                }
                        ));
            }
            messagesListAdapter.setMultiSelectMode(false);
            delegate.onSocialShare(false, 0);
        } else if (vid == R.id.btSharePic) {
            List<TextHolder> selectedItems = messagesListAdapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                ToastHelper.show(getContext(), "Nothing selected...");
            } else {
                ToastHelper.show(getContext(), "Share Pic...");
                List<ButtonConfig> data = new ArrayList<>();
                for (TextHolder holder : selectedItems) {
                    if (holder.isUserSelected()) {
                        data.add(new ButtonConfig(holder.message.getText(), R.layout.screenshot_item_user_msg, null));
                    }
                    if (holder.isAiSelected()) {
                        data.add(new ButtonConfig(holder.getAiFeedback().getFeedbackText(), R.layout.screenshot_item_ai_msg, null));
                    }
                    List<Song> songs = holder.getSelectedSongs();
                    if (songs != null && !songs.isEmpty()) {
                        for (Song song : songs) {
                            data.add(new ButtonConfig("", R.layout.screenshot_item_song, song));
                        }
                    }
                }
                Disposable disposable = PermissionRequestHandler
                        .requestWriteExternalStorage((Activity) this.getContext())
                        .andThen( // After permission is granted, execute the following operations
                                Observable.<Bitmap>create(emitter -> {
                                            OffscreenScreenshotHelper.INSTANCE.screenshot(
                                                    this.getContext(),
                                                    SocialShareHandler.getHeaderImage().getUrl(),
                                                    null, null,
                                                    data,
                                                    result -> {
                                                        emitter.onNext(result); // 发送成功结果
                                                        emitter.onComplete(); // 完成
                                                        return Unit.INSTANCE;

                                                    }, err -> {
                                                        emitter.onError(err);
                                                        return Unit.INSTANCE;
                                                    });

                                        })
                                        .subscribeOn(Schedulers.io())
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                bitmap -> {
                                    Uri bitmapURL = ImageSaveUtils.INSTANCE.saveBitmapToGallery(
                                            this.getContext(), // context
                                            bitmap,
                                            "img_" + System.currentTimeMillis(),
                                            Bitmap.CompressFormat.JPEG
                                    );
                                    if (bitmapURL != null) {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("image/*"); // 或具体类型如 "image/jpeg"
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURL);
                                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 临时权限

                                        this.getContext().startActivity(Intent.createChooser(shareIntent, "分享图片"));
                                    }
                                    bitmap.recycle();
                                    messagesListAdapter.setMultiSelectMode(false);
                                    delegate.onSocialShare(false, 0);
                                },
                                e -> {
                                    ToastHelper.show(this.getContext(), e.getMessage());
                                }
                        );
                dm.add(disposable);
            }

        } else if (vid == R.id.btCancel) {
            messagesListAdapter.setMultiSelectMode(false);
//            shareMenu.setVisibility(View.GONE);
            delegate.onSocialShare(false, 0);
        } else if (vid == R.id.btPreview) {
//            String htmlContent = "<html><body><h1>Hello WebView</h1><p>This is HTML content.</p></body></html>";
            try {
                List<TextHolder> selectedItems = messagesListAdapter.getSelectedItems();
                if (selectedItems.isEmpty()) {
                    ToastHelper.show(getContext(), "Nothing selected...");
                    return;
                }

                String htmlContent = TemplateUtils.loadTemplate(getContext(), "templates/template_share.html");
                htmlContent = htmlContent.replace("{{headerUrl}}", SocialShareHandler.getHeaderImage().getUrl());
                String aiTemplate = "        <div class=\"message-block assistant\">\n" +
                        "            <div class=\"message-content markdown-content\">%s</div>\n" +
                        "        </div>";
                String userTemplate = "<div class=\"message-block user\">\n" +
                        "            <div class=\"message-content\">%s</div>\n" +
                        "        </div>";
                StringBuilder content = new StringBuilder();
                for (TextHolder holder : selectedItems) {
                    if (holder.isUserSelected()) {
                        content.append(String.format(userTemplate, holder.message.getText()));
                    }
                    if (holder.isAiSelected()) {
                        content.append(String.format(aiTemplate, holder.getAiFeedback().getFeedbackText()));
                    }
                    List<Song> songs = holder.getSelectedSongs();
                    if (songs != null && !songs.isEmpty()) {
                        for (Song song : songs) {
                            content.append(TemplateUtils.buildSongHtml(song));
                        }
                    }
                }
                htmlContent = htmlContent.replace("{{shareData}}", content);
                ShareRequest request = ShareRequestKt.createShareRequest(selectedItems);
                String summary = selectedItems.get(0).getShareSummary(getContext());

                WebViewActivity.sharePreviewWithHtml(this.getContext(), htmlContent, getResources().getString(R.string.share_preview), (new Gson()).toJson(request, ShareRequest.class), summary);
                LogUploader.reportEvent(
                        "mod_share", List.of(
                                new KeyValuePair("share_msg_count", Integer.toString(selectedItems.size())),
                                new KeyValuePair("share_action", "20")
                        ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setupRefreshLayout() {
        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout);

        // 下拉刷新监听
        swipeRefreshLayout.setOnRefreshListener(() -> {
            messagesListAdapter.setHeader(false);
            swipeRefreshLayout.setLoadingMore(false);
            loadElder();
        });

        // 绑定RecyclerView
        swipeRefreshLayout.setupWithRecyclerView(messagesList);


        // 上拉加载监听
        swipeRefreshLayout.setOnLoadMoreListener(new LoadMoreSwipeRefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadLatestActive() {
                delegate.onLoadLatestActive();
            }

            @Override
            public void onLoadMore() {
                if (!messagesListAdapter.getHeader()) {
                    messagesListAdapter.setHeader(true);
                    swipeRefreshLayout.setLoadingMore(true);
                    loadLater();
                }
            }
        });
    }

    public void addListeners() {
        if (listenersAdded) {
            return;
        }
        listenersAdded = true;

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageAdded))
                .subscribe(networkEvent -> {
                    messagesList.post(() -> {
                        addNewMessageHolders(networkEvent.getMessage());
                    });
                }));

//        dm.add(ChatSDK.events().sourceOnSingle()
//                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
//                .filter(NetworkEvent.filterThreadEntityID(delegate.getThread().getEntityID()))
//                .subscribe(networkEvent -> {
//                    Progress progress = networkEvent.getProgress();
//                    if (progress != null && progress.error != null) {
//                        messagesList.post(() -> {
//                            ToastHelper.show(getContext(), progress.error.getLocalizedMessage());
//                        });
//                    }
//                }));

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageRemoved))
                .subscribe(networkEvent -> {
                    messagesList.post(() -> {
                        removeMessage(networkEvent.getMessage());
                    });
                }));

//        dm.add(ChatSDK.events().sourceOnSingle()
//                .filter(NetworkEvent.filterType(EventType.MessageUpdated))
//                .subscribe(networkEvent -> {
//                    messagesList.post(() -> {
//                        updateMessage(networkEvent.getMessage());
//                    });
//                }));
    }


    public void loadLater() {
        Long startId = 0L;

        // If there are already items in the list, load messages before oldest
        if (!messageHolders.isEmpty()) {
            startId = messageHolders.get(messageHolders.size() - 1).getMessage().getId();
//        } else {
//            startId = (delegate.getMessageId() != null && delegate.getMessageId() > 0)
//                    ? delegate.getMessageId() -1
//                    : 0L;
        }
        Log.e("AIExplore", "onLoadLater:" + startId);
        GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
        dm.add(
                handler.loadMessagesLater(startId, true)
                        .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                            return getMessageHoldersAsync(messages, false);
                        })
                        .observeOn(RX.main())
                        .subscribe(messages -> {
                            messagesListAdapter.setHeader(false);
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayout.setLoadingMore(false);
                            synchronize(() -> {
                                addNewMessageHolders(messages);
                            });
                        }, error -> {
                            messagesListAdapter.setHeader(false);
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayout.setLoadingMore(false);
                            Context context = getContext(); // 获取Context
                            if (context != null) {
                                Toast.makeText(
                                        context,
                                        "加载失败: " + error.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }));


    }

    public void loadElder() {
        Long startId;

        // If there are already items in the list, load messages before oldest
        if (!messageHolders.isEmpty()) {
            startId = messageHolders.get(0).getMessage().getId();
        } else {
            startId = 0L;
            String messageId = delegate.getMessageId();
            Log.e("loadmsg", "msgid:" + messageId);
            if (messageId != null && !messageId.isEmpty()) {
                Message msg = ChatSDK.db().fetchMessageWithEntityID(messageId);
                if (msg != null) {
                    startId = msg.getId() + 1;
                }
            }
        }
//        Logger.warn("onLoadElder:" + startId);
        GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
        dm.add(
                handler.loadMessagesEarlier(startId, true)
                        .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                            return getMessageHoldersAsync(messages, false);
                        })
                        .observeOn(RX.main())
                        .subscribe(messages -> {
                            synchronize(() -> {
//                                messagesListAdapter.setHeader(false);
                                swipeRefreshLayout.setRefreshing(false);
                                swipeRefreshLayout.setLoadingMore(false);
                                addElderMessageHolders(messages);
                            });
                        }, error -> {
//                            messagesListAdapter.setHeader(false);
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayout.setLoadingMore(false);
                            Context context = getContext(); // 获取Context
                            if (context != null) {
                                Toast.makeText(
                                        context, error.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }));


    }

    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        loadElder();
    }

    public void loadPendingMsg() {
        String messageId = delegate.getMessageId();

        if (messageId != null && !messageId.isEmpty()) {

        }
    }

    public boolean isLatestVisible() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
        int pos = layoutManager.findLastVisibleItemPosition();
        long maxId = messagesListAdapter.getItemId(pos);
        if (maxId <= 0) {
            maxId = messagesListAdapter.getItemId(pos - 1);
        }
        if (maxId > 0 && maxId == latestMsgId) {
            return true;
        }
        return false;
    }

    /**
     * Start means new messages to bottom of screen
     */
    protected void addNewMessageHolders(Message message) {
//        if (false&&WelcomeHolder.isWelcomeMsg(message)) {
//            messagesListAdapter.addNewMessage(new WelcomeHolder(message), () -> {
//                LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
//                messagesList.postDelayed(() -> {
////                    int pos = messagesListAdapter.getItemCount() - 1;
//                    layoutManager.scrollToPositionWithOffset(messagesListAdapter.getItemCount() - 2, 0);
//                }, 100);
//
//
//                return Unit.INSTANCE;
//            });
//        } else {
        MessageHolder holder = HolderProvider.INSTANCE.getMessageHolder(message);
        if (holder != null && !messageHolders.contains(holder)) {

            messageHolders.add(0, holder);

//            updatePreviousMessage(holder);
//            holder.updateReadStatus();
//            messagesListAdapter.addNewMessage(holder, null);

            latestMsgId = message.getId();
            messagesListAdapter.addNewMessage(holder, () -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
                messagesList.postDelayed(() -> {
//                    int pos = messagesListAdapter.getItemCount() - 1;
                    layoutManager.scrollToPositionWithOffset(messagesListAdapter.getItemCount() - 2, 0);
                }, 100);


                return Unit.INSTANCE;
            });
//            messagesListAdapter.addToStart(holder, scroll, true);
        } else {
            Logger.debug("Exists already");
        }
//        }

    }

    protected void addNewMessageHolders(List<MessageHolder> holders) {
        List<MessageHolder> toAdd = new ArrayList<>();
        for (MessageHolder holder : holders) {
            if (!messageHolders.contains(holder)) {
                messageHolders.add(holder);
                toAdd.add(holder);
            } else {
                Logger.error("We have a duplicate");
            }
        }
        messagesListAdapter.addNewMessage(toAdd, null);
    }

    protected void removeMessage(Message message) {
        MessageHolder holder = HolderProvider.INSTANCE.getExitsMessageHolder(message);
        if (holder != null) {
            messageHolders.remove(holder);
            messagesListAdapter.delMessage(holder, null);
        }
        HolderProvider.INSTANCE.removeMessageHolder(message);
    }


    /**
     * End means historic messages to top of screen
     */
    protected void addElderMessageHolders(List<MessageHolder> holders) {
        // Add to current holders at zero index
        // Newest first
        if (holders == null || holders.isEmpty()) {
            return;
        }
//        if (messageHolders.isEmpty()) {
//            Message message = holders.get(0).message;
//            messagesListAdapter.addNewMessage(new ExploreHolder(message),null);
//        }
//        Log.e("AIExplore", "addElderMessageHolders");
        boolean isInit = messageHolders.isEmpty();
        List<MessageHolder> toAdd = new ArrayList<>();
        for (MessageHolder holder : holders) {
            if (!messageHolders.contains(holder)) {
                messageHolders.add(0, holder);
                toAdd.add(0, holder);
            } else {
                Log.e("loadmsg", "We have a duplicate:" + holder.message.getId());
            }
        }
        messagesListAdapter.addHistoryMessages(toAdd, () -> {
            messagesList.setItemAnimator(null);
            LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
            if (isInit) {
                String messageId = delegate.getMessageId();
                if (messageId != null && !messageId.isEmpty()) {
                    Log.e("loadmsg", "pos:" + (toAdd.size() - 1) + ",messageId:" + latestMsgId);
                    layoutManager.scrollToPositionWithOffset(toAdd.size() - 1, 300);
                } else {
                    latestMsgId = toAdd.get(toAdd.size() - 1).message.getId();
                    messagesList.postDelayed(() -> {
//                        Log.e("loadmsg", "scrollToPosition.isInit:" + messagesListAdapter.getItemCount() + ",latestMsgId:" + latestMsgId);
                        layoutManager.scrollToPosition(messagesListAdapter.getItemCount() - 1);
                    }, 100);
                }
            } else {
                messagesList.postDelayed(() -> {
//                    Log.e("loadmsg", "scrollToPosition:" + toAdd.size());
                    layoutManager.scrollToPositionWithOffset(toAdd.size(), 300);
//                    scrollToPositionTop(toAdd.size(),300);
                }, 100);
            }

//            scrollToPositionTop(toAdd.size()+1,0);
//            messagesList.getLayoutManager().scrollToPosition(messagesListAdapter.getItemCount());
//            scrollToPositionTop(position, 300);
            return Unit.INSTANCE;
        });

        // Reverse order because we are adding to end
//        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
//        assert layoutManager != null;
//        int position = messagesListAdapter.getItemCount() == 0 ? 1 : layoutManager.findLastVisibleItemPosition();
//        messagesListAdapter.addHistoryMessages(toAdd, () -> {
//            messagesList.setItemAnimator(null);
////            scrollToPositionTop(position, 300);
//            return Unit.INSTANCE;
//        });
//        messagesListAdapter.addToEnd(toAdd, false, notify);
    }

    /**
     * 滚动到指定 position，并让 item 顶部贴近 RecyclerView 顶部
     *
     * @param targetPosition 目标位置
     * @param offsetPx       额外偏移量（例如 100px，让 item 顶部距离顶部 100px）
     */
    private void scrollToPositionTop(int targetPosition, int offsetPx) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
        if (layoutManager == null || messagesList == null) return;

        messagesList.post(() -> {
            View itemView = layoutManager.findViewByPosition(targetPosition);
            if (itemView != null) {
                int itemHeight = itemView.getHeight();
                int recyclerViewHeight = messagesList.getHeight();
                int offset = recyclerViewHeight - itemHeight - offsetPx;
                layoutManager.scrollToPositionWithOffset(targetPosition, offset);
            } else {
                // 如果 item 未渲染，先滚动到目标位置再微调
                layoutManager.scrollToPosition(targetPosition);
                messagesList.post(() -> {
                    View delayedView = layoutManager.findViewByPosition(targetPosition);
                    if (delayedView != null) {
                        int itemHeight = delayedView.getHeight();
                        int recyclerViewHeight = messagesList.getHeight();
                        int finalOffset = recyclerViewHeight - itemHeight - offsetPx;
                        layoutManager.scrollToPositionWithOffset(targetPosition, finalOffset);
                    }
                });
            }
        });
    }

    protected void synchronize(Runnable modifyList) {
        synchronize(modifyList, false);
    }

    protected void synchronize(Runnable modifyList, boolean sort) {

        long start = System.currentTimeMillis();

        if (messagesListAdapter != null) {
//            final List<MessageWrapper<?>> oldHolders = new ArrayList<>(messagesListAdapter.getItems());

            if (modifyList != null) {
                modifyList.run();
            }
        }

        long end = System.currentTimeMillis();
        long diff = end - start;
//        System.out.println("Diff: " + diff);

    }

    public void sortMessageHolders() {
        Collections.sort(messageHolders, (o1, o2) -> {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        });
    }

    public List<MessageHolder> getMessageHolders(final List<Message> messages, boolean reverse) {

        // Get the holders - they will be in asc order i.e. oldest at 0
        TimeLog log = new TimeLog("Get Holders - " + messages.size());

        final List<MessageHolder> holders = new ArrayList<>();
        for (Message message : messages) {
            MessageHolder holder = HolderProvider.INSTANCE.getMessageHolder(message);
            holders.add(holder);
        }
        if (reverse) {
            Collections.reverse(holders);
        }

        log.end();

        return holders;
    }

    public Single<List<MessageHolder>> getMessageHoldersAsync(final List<Message> messages, boolean reverse) {
        return Single.create((SingleOnSubscribe<List<MessageHolder>>) emitter -> {
            emitter.onSuccess(getMessageHolders(messages, reverse));
        }).subscribeOn(RX.computation()).observeOn(RX.main());
    }

    public void notifyDataSetChanged() {
        messagesListAdapter.notifyDataSetChanged();
    }

    public void clear() {
        if (messagesListAdapter != null) {
            messageHolders.clear();
            messagesListAdapter.clear();
        }
    }

    public void scrollToLatest() {
        messagesList.postDelayed(() -> {
            Log.e("loadmsg", "scrollToPosition:" + messagesListAdapter.getItemCount());
            messagesList.getLayoutManager().scrollToPosition(messagesListAdapter.getItemCount() - 1);
        }, 100);
    }

    public ChatAdapter getMessagesListAdapter() {
        return messagesListAdapter;
    }

    public void removeListeners() {
        dm.dispose();
        listenersAdded = false;
    }

}
