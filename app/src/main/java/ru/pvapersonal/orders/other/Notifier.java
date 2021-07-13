package ru.pvapersonal.orders.other;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.DetailActivity;
import ru.pvapersonal.orders.model.Status;
import ru.pvapersonal.orders.service.listeners.EventTypes;

import static android.content.Context.NOTIFICATION_SERVICE;

public class Notifier {

    private static final String CHANNEL_ID_QUEUE = "channel_id_queue";
    private static final int ID_QUEUE = 0;
    private static final String CHANNEL_ID_USER_ADD = "channel_id_user_add";
    private static final int ID_USER_ADD = 1;
    private static final String CHANNEL_ID_USER_EXIT = "channel_id_user_exit";
    private static final int ID_USER_EXIT = 2;
    private static final String CHANNEL_ID_ROOM_DELETED = "channel_id_room_deleted";
    private static final int ID_ROOM_DELETED = 3;
    private static final String CHANNEL_ID_STATUS_CHANGE = "channel_id_room_status_change";
    private static final int ID_STATUS_CHANGE = 4;

    public static void registerChannels(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                NotificationManager notificationManager =
                        (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID_QUEUE, ctx.getString(R.string.queue_updates),
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription(ctx.getString(R.string.queue_updates_desc));
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    notificationManager.createNotificationChannel(channel);

                    NotificationChannel userAddedChannel = new NotificationChannel(CHANNEL_ID_USER_ADD,
                            ctx.getString(R.string.user_add), NotificationManager.IMPORTANCE_LOW);
                    userAddedChannel.setDescription(ctx.getString(R.string.user_add_desc));
                    userAddedChannel.enableLights(true);
                    userAddedChannel.enableVibration(true);
                    notificationManager.createNotificationChannel(userAddedChannel);

                    NotificationChannel userExitChannel = new NotificationChannel(CHANNEL_ID_USER_EXIT,
                            ctx.getString(R.string.user_exit), NotificationManager.IMPORTANCE_LOW);
                    userExitChannel.setDescription(ctx.getString(R.string.user_exit_desc));
                    userExitChannel.enableLights(true);
                    userExitChannel.enableVibration(true);
                    notificationManager.createNotificationChannel(userExitChannel);

                    NotificationChannel roomDeleteChannel = new NotificationChannel(CHANNEL_ID_ROOM_DELETED,
                            ctx.getString(R.string.room_delete), NotificationManager.IMPORTANCE_DEFAULT);
                    roomDeleteChannel.setDescription(ctx.getString(R.string.room_delete_desc));
                    roomDeleteChannel.enableLights(true);
                    roomDeleteChannel.enableVibration(true);
                    notificationManager.createNotificationChannel(roomDeleteChannel);

                    NotificationChannel roomStatusChangeChannel = new NotificationChannel(CHANNEL_ID_STATUS_CHANGE,
                            ctx.getString(R.string.room_status_change), NotificationManager.IMPORTANCE_DEFAULT);
                    roomStatusChangeChannel.setDescription(ctx.getString(R.string.room_status_change_desc));
                    roomStatusChangeChannel.enableLights(true);
                    roomStatusChangeChannel.enableVibration(true);
                    notificationManager.createNotificationChannel(roomStatusChangeChannel);
                }
            }
        }catch (Exception e){
            //Just dont want to mess up from boot loading
        }
    }

    public static void notifyQueueAdded(Context ctx, JSONObject queueMessage) throws JSONException {
        Intent notificationIntent = new Intent(ctx, DetailActivity.class);
        notificationIntent.putExtra(DetailActivity.ROOM_ID, queueMessage.getInt("roomId"));
        PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_ID_QUEUE)
                        .setSmallIcon(R.drawable.ic_queue)
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent)
                        .setContentTitle(ctx.getString(R.string.queue_notify_title))
                        .setContentText(String.format(ctx.getString(R.string.queue_text_main),
                                queueMessage.getString("name")));
        NotificationManager notificationManager =
                (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        Notification n = builder.build();
        n.defaults |= Notification.DEFAULT_ALL;
        notificationManager.notify(ID_QUEUE, n);
    }

    public static void notifyUserAdded(Context ctx, JSONObject update) throws JSONException {
        if (!update.getBoolean("self")) {
            Intent notificationIntent = new Intent(ctx, DetailActivity.class);
            notificationIntent.putExtra(DetailActivity.ROOM_ID, update.getInt("roomId"));
            PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                    0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(ctx, CHANNEL_ID_USER_ADD)
                            .setSmallIcon(R.drawable.ic_group_add)
                            .setAutoCancel(true)
                            .setContentIntent(contentIntent)
                            .setContentTitle(ctx.getString(R.string.user_add))
                            .setContentText(String.format(ctx.getString(R.string.user_add_main),
                                    update.getString("name"),
                                    update.getString("userName")));
            NotificationManager notificationManager =
                    (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
            Notification n = builder.build();
            n.defaults |= Notification.DEFAULT_ALL;
            notificationManager.notify(ID_USER_ADD, n);
        }
    }

    public static void notifyUserExited(Context ctx, JSONObject update) throws JSONException {
        if (!update.getBoolean("self")) {
            Intent notificationIntent = new Intent(ctx, DetailActivity.class);
            notificationIntent.putExtra(DetailActivity.ROOM_ID, update.getInt("roomId"));
            PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                    0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(ctx, CHANNEL_ID_USER_EXIT)
                            .setSmallIcon(R.drawable.ic_group_exit)
                            .setAutoCancel(true)
                            .setContentIntent(contentIntent)
                            .setContentTitle(ctx.getString(R.string.user_exit))
                            .setContentText(String.format(ctx.getString(R.string.user_exit_main),
                                    update.getString("name"),
                                    update.getString("userName")));
            NotificationManager notificationManager =
                    (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
            Notification n = builder.build();
            n.defaults |= Notification.DEFAULT_ALL;
            notificationManager.notify(ID_USER_EXIT, n);
        }
    }

    public static void notifyRoomDeleted(Context ctx, JSONObject update) throws JSONException {
        Intent notificationIntent = new Intent(ctx, DetailActivity.class);
        notificationIntent.putExtra(DetailActivity.ROOM_ID, update.getInt("roomId"));
        PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_ID_ROOM_DELETED)
                        .setSmallIcon(R.drawable.ic_trash_bin)
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent)
                        .setContentTitle(ctx.getString(R.string.user_exit))
                        .setContentText(String.format(ctx.getString(R.string.room_delete_main),
                                update.getString("name")));
        NotificationManager notificationManager =
                (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        Notification n = builder.build();
        n.defaults |= Notification.DEFAULT_ALL;
        notificationManager.notify(ID_ROOM_DELETED, n);
    }

    public static void notifyStatusChanged(Context ctx, JSONObject update) throws JSONException {
        String content = null;
        if (update.has("eventData") && update.getJSONObject("eventData").has("status")) {
            switch (Status.valueOf(update.getJSONObject("eventData").getString("status"))) {
                case WAIT:
                    content = String.format(ctx.getString(R.string.status_main_wait), update.getString("name"));
                    break;
                case EXECUTING:
                    content = String.format(ctx.getString(R.string.status_main_executing), update.getString("name"));
                    break;
                case EXECUTED:
                    content = String.format(ctx.getString(R.string.status_main_executed), update.getString("name"));
                    break;
            }
        }
        if (content != null) {
            Intent notificationIntent = new Intent(ctx, DetailActivity.class);
            notificationIntent.putExtra(DetailActivity.ROOM_ID, update.getInt("roomId"));
            PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                    0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(ctx, CHANNEL_ID_STATUS_CHANGE)
                            .setSmallIcon(R.drawable.ic_info)
                            .setAutoCancel(true)
                            .setContentIntent(contentIntent)
                            .setContentTitle(ctx.getString(R.string.room_status_change))
                            .setContentText(content);
            NotificationManager notificationManager =
                    (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
            Notification n = builder.build();
            n.defaults |= Notification.DEFAULT_ALL;
            notificationManager.notify(ID_STATUS_CHANGE, n);
        }
    }

    public static void executeRoomUpdates(Context ctx, JSONObject update) throws JSONException {
        switch (EventTypes.valueOf(update.getString("eventName"))) {
            case USER_JOINED:
                notifyUserAdded(ctx, update);
                break;
            case USER_EXITED:
                notifyUserExited(ctx, update);
                break;
            case ROOM_DELETED:
                notifyRoomDeleted(ctx, update);
                break;
            case ROOM_STATUS_CHANGE:
                notifyStatusChanged(ctx, update);
                break;
            default:
                //nothing
                break;
        }
    }
}
