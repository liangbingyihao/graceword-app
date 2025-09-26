package sdk.chat.demo.robot.holder;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import org.pmw.tinylog.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import sdk.chat.core.dao.Message;
import sdk.chat.core.events.EventType;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.manager.ImageMessagePayload;
import sdk.chat.core.manager.MessagePayload;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.Progress;
import sdk.chat.core.types.ReadStatus;
import sdk.chat.core.utils.CurrentLocale;
import sdk.guru.common.DisposableMap;

public class MessageHolder implements IMessage, Consumer<Throwable> {

    public Message message;

    protected MessagePayload payload;

    protected Message nextMessage;
    protected Message previousMessage;

    protected boolean isGroup;
    protected boolean previousSenderEqualsSender;
    protected boolean nextSenderEqualsSender;
    protected boolean showDate;
    protected boolean isLast;
    protected String quotedImageURL;
    protected Drawable quotedImagePlaceholder;

    protected String typingText = null;
    protected DisposableMap dm = new DisposableMap();

    protected ReadStatus readStatus = null;

//    protected Date date;
    protected MessageSendStatus sendStatus = null;
    protected float transferPercentage = -1;
    protected float fileSize = -1;
    protected boolean isReply;

    protected boolean isDirty = true;

    public MessageHolder(Message message) {
        this.message = message;
        this.payload = ChatSDK.getMessagePayload(message);

        dm.add(ChatSDK.events().prioritySourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
                .filter(NetworkEvent.filterMessageEntityID(getId()))
                .subscribe(networkEvent -> {
                    Progress progress = networkEvent.getProgress();
                    updateProgress(progress);
                }, this));

        dm.add(ChatSDK.events().prioritySourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageSendStatusUpdated))
                .filter(NetworkEvent.filterMessageEntityID(getId()))
                .subscribe(networkEvent -> {
                    MessageSendStatus status = networkEvent.getMessageSendStatus();
                    updateSendStatus(status);
                }, this));

        dm.add(ChatSDK.events().prioritySourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageReadReceiptUpdated))
                .filter(NetworkEvent.filterMessageEntityID(getId()))
                .subscribe(networkEvent -> {
                    updateReadStatus();
                }, this));

        dm.add(ChatSDK.events().prioritySourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageUpdated))
                .filter(NetworkEvent.filterMessageEntityID(getId()))
                .subscribe(networkEvent -> {
                    updateNextAndPreviousMessages();
                }, this));

        updateSendStatus(message.getMessageStatus());
        updateProgress(null);
        updateNextAndPreviousMessages();

        isReply = message.isReply();

    }

    public void updateNextAndPreviousMessages() {
        Message nextMessage = message.getNextMessage();
        Message previousMessage = message.getPreviousMessage();

        boolean isLast = nextMessage == null;
        if (isLast != this.isLast) {
            this.isLast = isLast;
            isDirty = true;
        }

        if (!isDirty) {
            String oldNextMessageId = this.nextMessage != null ? this.nextMessage.getEntityID() : "";
            String newNextMessageId = nextMessage != null ? nextMessage.getEntityID() : "";
            isDirty = !oldNextMessageId.equals(newNextMessageId);
        }

        if (!isDirty) {
            String oldPreviousMessageId = this.previousMessage != null ? this.previousMessage.getEntityID() : "";
            String newPreviousMessageId = previousMessage != null ? previousMessage.getEntityID() : "";
            isDirty = !oldPreviousMessageId.equals(newPreviousMessageId);
        }

        this.nextMessage = nextMessage;
        this.previousMessage = previousMessage;

        previousSenderEqualsSender = previousMessage != null && message.getSender().equalsEntity(previousMessage.getSender());
        nextSenderEqualsSender = nextMessage != null && message.getSender().equalsEntity(nextMessage.getSender());

        DateFormat format = new SimpleDateFormat("dd-M-yyyy hh:mm", CurrentLocale.get(ChatSDK.ctx()));
        showDate = nextMessage == null || !(format.format(message.getDate()).equals(format.format(nextMessage.getDate())) && nextSenderEqualsSender);
        isGroup = message.getThread().typeIs(ThreadType.Group);

        Logger.warn("Message: " + message.getText() + ", showDate: " + showDate);
    }

    public void updateSendStatus(MessageSendStatus status) {
        isDirty = isDirty || status != sendStatus;
        sendStatus = status;
    }

    public void updateProgress(Progress progress) {
        if (progress == null) {

            isDirty = isDirty || this.transferPercentage >= 0;
            isDirty = isDirty || this.fileSize >= 0;

            transferPercentage = -1;
            fileSize = -1;

        } else {

            float uploadPercentage = Math.round(progress.asFraction() * 100);
            isDirty = isDirty || uploadPercentage == this.transferPercentage;
            this.transferPercentage = uploadPercentage;

            float fileSize = (float) Math.floor(progress.getTotalBytes() / 1000f);
            isDirty = isDirty || fileSize == this.fileSize;
            this.fileSize = fileSize;

        }
    }

    public void updateReadStatus() {
        ReadStatus status = message.getReadStatus();
        isDirty = isDirty || readStatus != status;
        readStatus = status;
    }

    @Override
    public String getId() {
        return message.getEntityID();
    }

    @Override
    public String getText() {
//        if (typingText != null) {
//            return typingText;
//        } else {
            return payload.getText();
//        }
    }

    @Override
    public String getPreview() {
        if (typingText != null) {
            return typingText;
        } else {
            return payload.lastMessageText();
        }
    }

    @Override
    public IUser getUser() {
        return null;
    }

    @Override
    public Date getCreatedAt() {
        return message.getDate();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof MessageHolder && getId().equals(((MessageHolder)object).getId());
    }

    public Message getMessage() {
        return message;
    }

    public MessageSendStatus getStatus() {
        return sendStatus;
    }

    public float getTransferPercentage() {
        return transferPercentage;
    }

    public float getFileSize() {
        return fileSize;
    }

//    public void setProgress(MessageSendProgress progress) {
//        this.progress = progress;
//    }

    public ReadStatus getReadStatus() {
        return readStatus;
    }

    public MessageSendStatus getSendStatus() {
        return sendStatus;
    }

    public boolean isReply() {
        return isReply;
    }

    public String getQuotedText() {
        if (payload.replyPayload() != null) {
            return payload.replyPayload().lastMessageText();
        }
        return null;
//        return message.getText();
    }

    public String getQuotedImageUrl() {
        return quotedImageURL;
    }

    public Drawable getQuotedPlaceholder() {
        return quotedImagePlaceholder;
    }



    public boolean showDate() {
        return showDate;
    }

    public String getIcon() {
        return payload.imageURL();
    }

    public static List<Message> toMessages(List<MessageHolder> messageHolders) {
        ArrayList<Message> messages = new ArrayList<>();
        for (MessageHolder mh: messageHolders) {
            messages.add(mh.getMessage());
        }
        return messages;
    }

    public boolean canResend() {
        return message.canResend();
    }

    public Single<String> save(final Context context) {
        return Single.just("");
    }

    public boolean canSave() {
        return false;
    }

    public void setTypingText(String text) {
        typingText = text;
    }

    public boolean isTyping() {
        return typingText != null;
    }


    public void makeClean() {
        isDirty = false;
    }

    public Message previousMessage() {
        return previousMessage;
    }

    public Message nextMessage() {
        return nextMessage;
    }

    @Override
    public void accept(Throwable throwable) throws Exception {
        throwable.printStackTrace();
    }

//    public @DrawableRes int defaultPlaceholder() {
//        return R.drawable.icn_100_profile;
//    }

    public MessagePayload getPayload() {
        return payload;
    }

    public boolean isLast() {
        return isLast;
    }

    public boolean enableLinkify() {
        return true;
    }
}
