package ru.pvapersonal.orders.service.listeners;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.pvapersonal.orders.other.App;
import ru.pvapersonal.orders.other.Notifier;

public abstract class GeneralEventListener {

    private Long qDate;
    private FragmentActivity activity;

    public GeneralEventListener(Long queryDate, FragmentActivity activity) {
        this.activity = activity;
        qDate = queryDate;
    }

    public void executeUpdates(JSONArray generalList, JSONArray queueList, Context serviceContext) {
        new Handler(activity.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < generalList.length(); i++) {
                        JSONObject upd = generalList.getJSONObject(i);
                        if (upd.getLong("eventDate") > qDate) {
                            switch (EventTypes.valueOf(generalList.getJSONObject(i).getString("eventName"))) {
                                case ROOM_CREATED:
                                    roomCreated(upd);
                                    break;
                                case USER_EXITED:
                                    roomUserExited(upd);
                                    break;
                                case USER_JOINED:
                                    roomUserJoined(upd);
                                    break;
                                case ROOM_DELETED:
                                    roomDeleted(upd);
                                    break;
                                case ROOM_INFO_EDITED:
                                    roomEdited(upd);
                                    break;
                                case ROOM_STATUS_CHANGE:
                                    roomStatusEdited(upd);
                                    break;
                                case QUEUE_TOGGLE:
                                    queueToggle(upd);
                                    break;
                                case QUEUE_USER_ADDED:
                                    queueAdded(upd);
                                    break;
                                case QUEUE_USER_REMOVED:
                                    queueRemoved(upd);
                                    break;
                                default:
                                    //Do nothing
                                    break;
                            }
                        }
                    }
                    for (int i = 0; i < queueList.length(); i++) {
                        JSONArray roomArr = queueList.getJSONArray(i);
                        for (int j = 0; j < roomArr.length(); j++) {
                            Notifier.notifyQueueAdded(serviceContext, roomArr.getJSONObject(i));
                        }
                    }
                } catch (JSONException e) {
                    Log.d("Orders", "Unrecognizable update came");
                }
            }
        });


    }

    public abstract void roomDeleted(JSONObject delete) throws JSONException;

    public abstract void roomEdited(JSONObject edit) throws JSONException;

    public abstract void roomCreated(JSONObject create) throws JSONException;

    public abstract void roomUserExited(JSONObject userExited) throws JSONException;

    public abstract void roomUserJoined(JSONObject userJoined) throws JSONException;

    public abstract void roomStatusEdited(JSONObject statusChanged) throws JSONException;

    public abstract void queueToggle(JSONObject queryToggle) throws JSONException;

    public abstract void queueAdded(JSONObject queryAdded) throws JSONException;

    public abstract void queueRemoved(JSONObject queryRemoved) throws JSONException;
}
