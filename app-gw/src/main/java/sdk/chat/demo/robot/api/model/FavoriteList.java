package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import sdk.chat.demo.robot.extensions.DateLocalizationUtil;


public class FavoriteList {
    private List<FavoriteItem> items;

    public List<FavoriteList.FavoriteItem> getItems() {
        return items;
    }

    public void setItems(List<FavoriteList.FavoriteItem> items) {
        this.items = items;
    }

    public static class FavoriteItem {
        @SerializedName("message_id")
        private String messageId;
        @SerializedName("content_type")
        private Integer contentType;
        @SerializedName("session_name")
        private String sessionName;
        @SerializedName("created_ts")
        private Long createdTs;
        private String content;
        private List<Song> songs;
        private String createAt;

        private boolean isExpanded;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public Integer getContentType() {
            return contentType;
        }

        public void setContentType(Integer contentType) {
            this.contentType = contentType;
        }

        public String getCreatedAt() {
            if (createdTs != null && (createAt == null || createAt.isEmpty())) {
                createAt = DateLocalizationUtil.INSTANCE.dateStr(new Date(createdTs));
            }
            return createAt;
        }

        public Long getCreatedTs() {
            return createdTs;
        }

        public void setCreatedTs(Long createdTs) {
            this.createdTs = createdTs;
        }

        public String getContent() {
            return content;
        }

        public String getText() {
            if (songs != null && !songs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Song s : songs) {
                    sb.append(s.getTitle()).append("\n\n");
                    sb.append(s.getLyrics()).append("\n");
                }
                return sb.toString();
            } else {
                return content;
            }
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        public void setExpanded(boolean expanded) {
            isExpanded = expanded;
        }

        public String getSessionName() {
            return sessionName;
        }

        public void setSessionName(String sessionName) {
            this.sessionName = sessionName;
        }

        public List<Song> getSongs() {
            return songs;
        }

        public void setSongs(List<Song> songs) {
            this.songs = songs;
        }
    }
}

