package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.handlers.GWThreadHandler;

public class ImageDaily {
    private String scripture;
    private String url;
    private String date;
    private String reference;
    @SerializedName("background_url")
    private String backgroundUrl;
    private ArrayList<String> explore;

    private String greeting;
    private String fontUrl;
//    private ArrayList<String> prompt;


    public ImageDaily(String backgroundUrl, String date, String reference, String scripture) {
        this.backgroundUrl = backgroundUrl;
        this.date = date;
        this.reference = reference;
        this.scripture = scripture;
    }

    public ImageDaily(String scripture, String backgroundUrl) {
        this.scripture = scripture;
        this.backgroundUrl = backgroundUrl;
    }

    public ImageDaily(String scripture, String backgroundUrl,String greeting) {
        this.scripture = scripture;
        this.backgroundUrl = backgroundUrl;
        this.greeting = greeting;
    }

    public String getScripture() {
        return scripture;
    }

    public void setScripture(String scripture) {
        this.scripture = scripture;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }

    public ArrayList<String> getExplore() {
        return explore;
    }

    public void setExplore(ArrayList<String> explore) {
        this.explore = explore;
    }

    public List<AIExplore.ExploreItem> getExploreWithParams() {

        List<AIExplore.ExploreItem> function = new ArrayList<>();
        for (String e : explore) {
            function.add(new AIExplore.ExploreItem(0,null,e));
        }

        return function;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getFontUrl() {
        return fontUrl;
    }

    public void setFontUrl(String fontUrl) {
        this.fontUrl = fontUrl;
    }

    //    public ArrayList<String> getPrompt() {
//        return prompt;
//    }
//
//    public void setPrompt(ArrayList<String> prompt) {
//        this.prompt = prompt;
//    }
}
