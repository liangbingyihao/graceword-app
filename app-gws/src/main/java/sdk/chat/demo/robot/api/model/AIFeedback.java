package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import sdk.chat.demo.robot.adpter.data.AIExplore;

public class AIFeedback {
    @SerializedName("color_tag")
    private String colorTag;
    private List<AIExplore.ExploreItem> functions;
    private String topic;
    private String tag;
    private String bible;
    private String view;
    private List<String> explore;
    private String prompt;

    public String getColorTag() {
        return colorTag;
    }

    public void setColorTag(String colorTag) {
        this.colorTag = colorTag;
    }

    public List<AIExplore.ExploreItem> getFunctions() {
        return functions;
    }

    public void setFunctions(List<AIExplore.ExploreItem> functions) {
        this.functions = functions;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBible() {
        return bible;
    }

    public void setBible(String bible) {
        this.bible = bible;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<String> getExplore() {
        return explore;
    }

    public void setExplore(List<String> explore) {
        this.explore = explore;
    }
}
