package sdk.chat.demo.robot.holder;


import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sdk.chat.core.dao.Message;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.GWConfigs;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.handlers.GWMsgHandler;


public class ExploreHolder implements IMessage {
    public Message message;
    private AIExplore aiExplore;


    public ExploreHolder(Message message) {
        this.message = message;
        MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
        if (aiFeedback != null && aiFeedback.getFeedback() != null) {
            aiExplore = AIExplore.loads(message);
        } else if (WelcomeHolder.isWelcomeMsg(message)) {
            GWConfigs.WelcomeSurvey welcomeSurvey = ImageApi.getGwConfigs().getWelcomeSurvey();
            if (welcomeSurvey != null) {
                GWConfigs.WelcomeSurveyOption option = welcomeSurvey.findOptionByValue(message.getReply());
                List<AIExplore.ExploreItem> itemList = new ArrayList<>();
                if (option != null && option.getPrompts() != null) {
                    for (String prompt : option.getPrompts()) {
                        itemList.add(new AIExplore.ExploreItem(AIExplore.ExploreItem.action_input_prompt_welcome, null, prompt));
                    }
                }
                aiExplore = new AIExplore(message, itemList);
            }
        }
    }
//
//    public ExploreHolder(IMessage holder) {
//        if(holder instanceof MessageHolder){
//            this.message = ((MessageHolder)holder).message;
//            MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
//            if (aiFeedback != null && aiFeedback.getFeedback() != null) {
//                aiExplore = AIExplore.loads(message);
//            }
//        } else if (holder instanceof WelcomeHolder){
//            WelcomeHolder h = ((WelcomeHolder)holder);
//            this.message = h.message;
//            aiExplore = h.getAiExplore();
//        }
//
//    }

//    public Message getMessage() {
//        return message;
//    }
//
//    public void setMessage(Message message) {
//        this.message = message;
//
//        MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
//        if (aiFeedback != null && aiFeedback.getFeedback() != null) {
//            aiExplore = AIExplore.loads(message);
//        }
//    }

    public AIExplore getAiExplore() {
        if (aiExplore == null && !WelcomeHolder.isWelcomeMsg(message)) {
            MessageDetail aiFeedback = GWMsgHandler.getAiFeedback(message);
            if (aiFeedback != null && aiFeedback.getFeedback() != null) {
                aiExplore = AIExplore.loads(message);
            }
        }
        return aiExplore;
    }

    public void setAiExplore(AIExplore aiExplore) {
        this.aiExplore = aiExplore;
    }

    @Override
    public String getId() {
        return "explore_" + message.getEntityID();
    }

    @Override
    public String getText() {
        return "";
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
