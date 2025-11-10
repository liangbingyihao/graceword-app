package sdk.chat.demo.robot.holder;


import com.stfalcon.chatkit.commons.models.IUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sdk.chat.core.dao.Message;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.GWConfigs;


public class WelcomeHolder extends TextHolder {
    public Message message;
    private GWConfigs.WelcomeSurveyOption option;
    private String question;
//    private AIExplore aiExplore;
    public final static String WELCOME_MSG_ID = "welcome_beginner";


    public WelcomeHolder(Message message) {
        super(message);
        GWConfigs.WelcomeSurvey welcomeSurvey = ImageApi.getGwConfigs().getWelcomeSurvey();
        if (welcomeSurvey != null) {
            question = welcomeSurvey.getQuestion();
            option = welcomeSurvey.findOptionByValue(message.getReply());

//            List<AIExplore.ExploreItem> itemList = new ArrayList<>();
//            if (option != null && option.getPrompts() != null) {
//                for (String prompt : option.getPrompts()) {
//                    itemList.add(new AIExplore.ExploreItem(AIExplore.ExploreItem.action_input_prompt_welcome, null, prompt));
//                }
//            }
//            aiExplore = new AIExplore(message, itemList);
        }

    }

    public static Message getWelcomeMessage(String reply) {
        Message message = ChatSDK.db().fetchMessageWithEntityID(WELCOME_MSG_ID);
        if(message==null){
            message = new Message();
            message.setEntityID(WELCOME_MSG_ID);
            message.setSender(ChatSDK.currentUser());
            message.setDate(new Date(1640995200000L));
            message.setReply(reply);
            message.setType(MessageType.Text);
            message.setMessageStatus(MessageSendStatus.Sent,false);
            ChatSDK.db().insertOrReplaceEntity(message);
        }
        return message;
    }

    public static boolean isWelcomeMsg(Message message) {
        return WELCOME_MSG_ID.equals(message.getEntityID());
    }

    public String getQuestion() {
        return question;
    }

    public GWConfigs.WelcomeSurveyOption getOption() {
        return option;
    }

//    public AIExplore getAiExplore() {
////        if (aiExplore == null) {
////            MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
////            if (aiFeedback != null && aiFeedback.getFeedback() != null) {
////                aiExplore = AIExplore.loads(message);
////            }
////        }
//        return aiExplore;
//    }
//
//    public void setAiExplore(AIExplore aiExplore) {
//        this.aiExplore = aiExplore;
//    }

    @Override
    public String getId() {
        return WELCOME_MSG_ID;
    }

    @Override
    public String getText() {
        return message.getReply();
    }

    @Override
    public String getPreview() {
        return "";
    }

    @Override
    public IUser getUser() {
        return null;
    }

    @Override
    public Date getCreatedAt() {
        return null;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void makeClean() {

    }
}
