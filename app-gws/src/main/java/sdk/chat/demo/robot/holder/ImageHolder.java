package sdk.chat.demo.robot.holder;


import androidx.annotation.Nullable;

import com.stfalcon.chatkit.commons.models.MessageContentType;

import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.ImageDaily;
public class ImageHolder extends MessageHolder implements MessageContentType, MessageContentType.Image {
    private String bibleDate;
    private ImageDaily imageDaily;
    //    private String bible;
//    private String imageUrl;
    private final int action;

    public ImageHolder(Message message) {
        super(message);
        action = message.integerForKey("action");
        if (!message.stringForKey(Keys.ImageUrl).isEmpty()) {
            imageDaily = new ImageDaily(message.stringForKey("image-text"), message.stringForKey(Keys.ImageUrl));
        } else if (action == AIExplore.ExploreItem.action_daily_gw || action == AIExplore.ExploreItem.action_daily_gw_pray) {
            bibleDate = message.stringForKey("image-date");
        }
    }

    public int getAction() {
        return action;
    }

    public ImageDaily getImageDaily() {
        if (imageDaily == null && (action == AIExplore.ExploreItem.action_daily_gw || action == AIExplore.ExploreItem.action_daily_gw_pray)) {
            imageDaily = ImageApi.getImageDailyListCache(bibleDate);
        }
        return imageDaily;
    }

//    public String getBible() {
//        return bible;
//    }
//
//    @Nullable
//    @Override
//    public String getImageUrl() {
//        return imageUrl;
//    }

    public String getText() {
        return message.getText();
    }

    public void updateNextAndPreviousMessages() {
//        this.isLast = false;
//        if (!this.isLast) {
//            return;
//        }
//        Message nextMessage = message.getNextMessage();
//        Message previousMessage = message.getPreviousMessage();
//
//        boolean isLast = nextMessage == null;
//        if (isLast != this.isLast) {
//            this.isLast = isLast;
//            isDirty = true;
//        }
//
//        if (!isDirty) {
//            String oldNextMessageId = this.nextMessage != null ? this.nextMessage.getEntityID() : "";
//            String newNextMessageId = nextMessage != null ? nextMessage.getEntityID() : "";
//            isDirty = !oldNextMessageId.equals(newNextMessageId);
//        }
//
//        if (!isDirty) {
//            String oldPreviousMessageId = this.previousMessage != null ? this.previousMessage.getEntityID() : "";
//            String newPreviousMessageId = previousMessage != null ? previousMessage.getEntityID() : "";
//            isDirty = !oldPreviousMessageId.equals(newPreviousMessageId);
//        }
//
//        this.nextMessage = nextMessage;
//        this.previousMessage = previousMessage;
//
//        previousSenderEqualsSender = previousMessage != null && message.getSender().equalsEntity(previousMessage.getSender());
//        nextSenderEqualsSender = nextMessage != null && message.getSender().equalsEntity(nextMessage.getSender());
//
//        DateFormat format = UIModule.shared().getMessageBinder().messageTimeComparisonDateFormat(ChatSDK.ctx());
//        showDate = nextMessage == null || !(format.format(message.getDate()).equals(format.format(nextMessage.getDate())) && nextSenderEqualsSender);
////        isGroup = message.getThread().typeIs(ThreadType.Group);
//
//        Logger.warn("Message: " + message.getText() + ", showDate: " + showDate);
    }

    public void updateReadStatus() {
    }

    @Nullable
    @Override
    public String getImageUrl() {
        return "";
    }
}
