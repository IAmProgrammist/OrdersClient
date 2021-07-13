package ru.pvapersonal.orders.service.listeners;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import ru.pvapersonal.orders.other.Notifier;

public abstract class RoomListener {
    private final Long timestamp = System.currentTimeMillis();
    private long qDate;
    private int roomId;

    public RoomListener(int roomId, long queryDate) {
        this.roomId = roomId;
        qDate = queryDate;
    }

    public void executeUpdate(Context ctx, JSONObject update) throws JSONException {
        if(update.getLong("eventDate") > qDate) {
            switch (EventTypes.valueOf(update.getString("eventName"))) {
                case USER_JOINED:
                    if (!userJoined(update)) {
                        Notifier.notifyUserAdded(ctx, update);
                    }
                    break;
                case USER_EXITED:
                    if (!userExited(update)) {
                        Notifier.notifyUserExited(ctx, update);
                    }
                    break;
                case ROOM_INFO_EDITED:
                    roomEdited(update);
                    break;
                case ROOM_DELETED:
                    if (!roomDeleted(update)) {
                        Notifier.notifyRoomDeleted(ctx, update);
                    }
                case ROOM_STATUS_CHANGE:
                    if (!statusChanged(update)) {
                        Notifier.notifyStatusChanged(ctx, update);
                    }
                    break;
                default:
                    //Do nothing
                    break;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomListener that = (RoomListener) o;
        return Objects.equals(timestamp, that.timestamp) && roomId == that.roomId && qDate==that.qDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, roomId, qDate);
    }

    public abstract boolean userJoined(JSONObject userJoined);

    public abstract boolean userExited(JSONObject userExited);

    public abstract boolean roomEdited(JSONObject roomEdited) throws JSONException;

    public abstract boolean roomDeleted(JSONObject delete);

    public abstract boolean statusChanged(JSONObject statusChange) throws JSONException;
}
