package sdk.chat.demo.robot.holder;


import com.stfalcon.chatkit.commons.models.MessageContentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import kotlin.jvm.JvmField;
import sdk.chat.core.dao.Message;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.model.AIFeedback;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.api.model.Song;
import sdk.chat.demo.robot.handlers.GWMsgHandler;
import sdk.chat.demo.robot.handlers.GWThreadHandler;

public class TextHolder extends MessageHolder implements MessageContentType, AIFeedbackType {
    @JvmField
    private MessageDetail aiFeedback;
    private final int action;
    public final boolean isSong;

    public TextHolder(Message message) {
        super(message);
        action = message.integerForKey("action");
        isSong = action == AIExplore.ExploreItem.action_search_hymns;
    }

    public int getAction() {
        return action;
    }

    public void setAiFeedback(MessageDetail aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public MessageDetail getAiFeedback() {
        if (aiFeedback == null && !message.stringForKey(GWThreadHandler.KEY_AI_FEEDBACK).isEmpty()) {
            aiFeedback = GWMsgHandler.getAiFeedback(message);
        }
        return aiFeedback;
    }

    public List<Song> getSongs() {
        MessageDetail messageDetail = getAiFeedback();
        if (isSong && messageDetail != null) {
            return messageDetail.getFeedback().getHymns();
        } else {
            return null;
        }
    }

    public boolean hasSelectedHymns() {

        // 多层空值检查
        if (!isSong) {
            return false;
        }

        MessageDetail messageDetail = getAiFeedback();
        if (messageDetail == null) {
            return false;
        }

        AIFeedback aiFeedback = messageDetail.getFeedback();
        if (aiFeedback == null) {
            return false;
        }

        List<Song> hymns = aiFeedback.getHymns();
        if (hymns == null || hymns.isEmpty()) {
            return false;
        }

        // 检查是否有选中的歌曲
        for (Song song : hymns) {
            if (song != null && song.isSelected()) {
                return true;
            }
        }

        return false;
    }

    public List<Song> getSelectedSongs() {
        return isSong ?
                Optional.ofNullable(getAiFeedback())
                        .map(MessageDetail::getFeedback)
                        .map(AIFeedback::getHymns)
                        .map(hymns -> hymns.stream()
                                .filter(song -> song != null && song.isSelected())
                                .collect(Collectors.toList()))
                        .orElse(new ArrayList<>()) :
                new ArrayList<>();

//        List<Song> selectedSongs = new ArrayList<>();
//
//        // 多层空值检查
//        if (!isSong) {
//            return selectedSongs;
//        }
//
//        MessageDetail messageDetail = getAiFeedback();
//        if (messageDetail == null) {
//            return selectedSongs;
//        }
//
//        AIFeedback aiFeedback = messageDetail.getFeedback();
//        if (aiFeedback == null) {
//            return selectedSongs;
//        }
//
//        List<Song> hymns = aiFeedback.getHymns();
//        if (hymns == null || hymns.isEmpty()) {
//            return selectedSongs;
//        }
//
//        // 检查是否有选中的歌曲
//        for (Song song : hymns) {
//            if (song != null && song.isSelected()) {
//                selectedSongs.add(song);
//            }
//        }
//
//        return selectedSongs;
    }

    public void updateNextAndPreviousMessages() {
        this.isLast = false;
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

}
