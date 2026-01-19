package sdk.chat.demo.robot.api.model;

import java.util.List;

import sdk.chat.core.dao.Message;

public class MessagePage {
    private List<Message> items;
    private boolean hasMore;

    public MessagePage(List<Message> items,boolean hasMore) {
        this.hasMore = hasMore;
        this.items = items;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<Message> getItems() {
        return items;
    }

    public void setItems(List<Message> items) {
        this.items = items;
    }
}
