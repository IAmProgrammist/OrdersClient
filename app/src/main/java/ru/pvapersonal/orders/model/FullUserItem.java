package ru.pvapersonal.orders.model;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class FullUserItem implements Comparable<FullUserItem> {
    public int userId;
    public String name = null;
    public String image = null;
    public String phone;
    public Long lastAction = null;
    public Boolean isAdmin = null;
    public Long qDate = null;

    public FullUserItem(JSONObject obj) throws JSONException {
        userId = obj.getInt("userId");
        if (obj.has("name")) {
            name = obj.getString("name");
        }
        if (obj.has("image")) {
            image = obj.getString("image");
        }
        phone = obj.getString("phone");
    }

    public FullUserItem(JSONObject obj, Long qDate) throws JSONException {
        userId = obj.getInt("userId");
        this.qDate = qDate;
        if (obj.has("name")) {
            name = obj.getString("name");
        }
        if (obj.has("image")) {
            image = obj.getString("image");
        }
        if(obj.has("lastAction")){
            lastAction = obj.getLong("lastAction");
        }
        if(obj.has("isAdmin")){
            isAdmin = obj.getBoolean("isAdmin");
        }
        phone = obj.getString("phone");
    }

    public FullUserItem(JSONObject obj, boolean self) throws JSONException {
        userId = obj.getInt("userId");
        if (obj.has("name") || !self) {
            name = obj.getString("name");
        }
        if (obj.has("image")) {
            image = obj.getString("image");
        }
        phone = obj.getString("phone");
    }

    public FullUserItem(int userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullUserItem that = (FullUserItem) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public int compareTo(FullUserItem o) {
        if(name == null){
            return -1;
        }
        if(o.name == null){
            return 1;
        }
        if(isAdmin != null && isAdmin){
            return -1;
        }
        if(o.isAdmin!= null &&o.isAdmin){
            return 1;
        }
        return name.compareTo(o.name);
    }

    public boolean areContentsSame(FullUserItem newItem) {
        return name.compareTo(newItem.name) == 0 && phone.compareTo(newItem.phone) == 0 &&
                image.compareTo(newItem.image) == 0 && userId == newItem.userId;
    }

    public Uri getPhoneCallUri() {
        return Uri.parse("tel:" + phone);
    }

    public Uri getWhatsAppCallUri() {
        return Uri.parse("https://api.whatsapp.com/send?phone=" + phone);
    }

    public Uri getViberCallUri() {
        return Uri.parse("viber://add?number=" + phone);
    }

    public String getLastActionText() {
        if(lastAction != null && qDate != null){
            return "Последнее действие: " + new SimpleDateFormat("dd MMMM yyyy HH:mm",
                    new Locale("RU")).format(lastAction) +
                    String.format(new Locale("RU"), " (%d м. с последнего действия)",
                            (Math.abs(qDate-lastAction)) / 60000L);
        }else{
            return "";
        }
    }
}
