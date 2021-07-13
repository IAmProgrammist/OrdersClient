package ru.pvapersonal.orders.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class QueueItem {
    public String userName;
    public Long registartionDate;
    public int userId;
    public boolean enabled;
    public boolean self;

    public QueueItem(JSONObject it) throws JSONException {
        userName = it.getString("userName");
        registartionDate = it.getLong("date");
        userId = it.getInt("userId");
        enabled = it.getBoolean("waiting");
        self = it.has("self") && it.getBoolean("self");
    }

    public QueueItem(int userId){
        this.userId = userId;
    }

    public QueueItem(String userName, Long registartionDate, int userId, boolean enabled, boolean self) {
        this.userName = userName;
        this.registartionDate = registartionDate;
        this.userId = userId;
        this.enabled = enabled;
        this.self = self;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueItem queueItem = (QueueItem) o;
        return userId == queueItem.userId;
    }

    public boolean areItemsTheSame(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QueueItem queueItem = (QueueItem) o;
        return userId == queueItem.userId &&
                enabled == queueItem.enabled &&
                self == queueItem.self &&
                Objects.equals(userName, queueItem.userName) &&
                Objects.equals(registartionDate, queueItem.registartionDate);
    }

    public boolean areContentsSame(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QueueItem queueItem = (QueueItem) o;
        return userId == queueItem.userId &&
                enabled == queueItem.enabled &&
                Objects.equals(userName, queueItem.userName) &&
                Objects.equals(registartionDate, queueItem.registartionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, registartionDate, userId, enabled);
    }

    public String getTime() {
        return new SimpleDateFormat("dd MMMM yyyy года HH:mm",new Locale("RU"))
                .format(new Date(registartionDate));
    }
}
