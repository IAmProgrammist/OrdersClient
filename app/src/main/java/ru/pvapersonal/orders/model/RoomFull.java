
package ru.pvapersonal.orders.model;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.UserInfoActivity;

public class RoomFull {
    public List<ShortUser> members;
    public boolean passworded;
    public long maxMembers;
    public int creatorId;
    public boolean shouldShowExpandedKeyboard;
    public int participiantType;
    public String creatorName;
    public Status st;
    public long start;
    public long end;
    public long qDate;
    public int id;
    public String roomName;
    public boolean isAdmin;
    public Long creationDate;
    public Long delta;
    public int transType;
    public long transVal;
    public String comment;

    public RoomFull(JSONObject res) throws JSONException {
        qDate = res.getLong("queryDate");
        delta = System.currentTimeMillis() - qDate;
        id = res.getInt("id");
        roomName = res.getString("name");
        isAdmin = res.getBoolean("isAdmin");
        creationDate = res.getLong("creationDate");
        members = new ArrayList<>();
        if (res.has("users")) {
            JSONArray users = res.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                members.add(new ShortUser(users.getJSONObject(i)));
            }
        }
        passworded = res.getBoolean("passworded");
        maxMembers = res.getInt("maxMembers");
        creatorId = res.getInt("creatorId");
        participiantType = res.getInt("participantType");
        creatorName = res.getString("creator");
        st = Status.valueOf(res.getString("status"));
        start = res.has("start") ? res.getLong("start") : -1;
        end = res.has("end") ? res.getLong("end") : -1;
        transType = res.has("payType") ? res.getInt("payType") : -1;
        transVal = res.has("payVal") ? res.getLong("payVal") : -1;
        comment = res.has("comment") ? res.getString("comment") : null;
        shouldShowExpandedKeyboard = res.getBoolean("shouldShowExpandedKeyboard");
    }

    public int getMembersSize() {
        return members.size();
    }

    public void addMember(JSONObject newUser, boolean self) throws JSONException {
        members.add(new ShortUser(newUser, self));
    }

    public void removeMember(int toRemoveId) {
        List<ShortUser> newMembers = new ArrayList<>();
        for (ShortUser user : members) {
            if (user.userId != toRemoveId) {
                newMembers.add(user);
            }
        }
        members = newMembers;
    }

    public List<ShortUser> getMembers() {
        return members;
    }

    public String getExecutionDate() {
        if (start == -1 || end == -1) {
            return "";
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("RU"));
            return simpleDateFormat.format(new Date(start)) + " — " + simpleDateFormat.format(new Date(end));
        }
    }

    public String getPayType(String[] stringArray) {
        if (transType != -1) {
            return stringArray[transType];
        }
        return "";
    }

    public String getPayVal(String[] suffixes) {
        String toReturn;
        switch (transType) {
            case 0:
                toReturn = suffixes[transType];
                long gcd = GCD((end - start), 3600000L);
                long precount = transVal * ((end - start) / gcd) / (3600000L / gcd);
                toReturn = String.format(toReturn, (transVal / 100 + "." + transVal % 100),
                        (precount / 100 + "." + precount % 100));
                break;
            case 1:
                toReturn = suffixes[transType];
                toReturn = String.format(toReturn, (transVal / 100 + "." + transVal % 100),
                        (transVal / maxMembers / 100 + "." + transVal / maxMembers % 100), maxMembers);
                break;
            default:
                toReturn = "";
                break;
        }
        return toReturn;
    }

    public long GCD(long a, long b) {
        if (b == 0) return a;
        return GCD(b, a % b);
    }

    public static class ShortUser implements Comparable<ShortUser> {
        public int userId;
        public String shortName;
        public String avatar;
        public boolean self;

        public ShortUser(JSONObject object, boolean self) throws JSONException {
            userId = object.getInt("userId");
            if (object.has("shortname")) {
                shortName = object.getString("shortname");
            } else if (object.has("name")) {
                shortName = object.getString("name");
            } else {
                shortName = "";
            }
            if (object.has("image")) {
                avatar = object.getString("image");
            } else if (object.has("avatar")) {
                avatar = object.getString("avatar");
            } else {
                avatar = null;
            }
            this.self = self;
        }

        public ShortUser(JSONObject object) throws JSONException {
            userId = object.getInt("userId");
            if (object.has("shortname")) {
                shortName = object.getString("shortname");
            } else if (object.has("name")) {
                shortName = object.getString("name");
            } else {
                shortName = "";
            }
            if (object.has("image")) {
                avatar = object.getString("image");
            } else if (object.has("avatar")) {
                avatar = object.getString("avatar");
            } else {
                avatar = null;
            }
            self = object.getBoolean("self");
        }

        public ShortUser() {
            userId = 1;
            shortName = "";
            avatar = null;
            self = true;
        }

        @Override
        public int compareTo(ShortUser o) {
            if (self && !o.self) {
                return -1;
            }
            if (!self && o.self) {
                return 1;
            }
            return shortName.compareTo(o.shortName);
        }

        public boolean areContentsSame(ShortUser o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShortUser user = (ShortUser) o;
            return userId == user.userId &&
                    self == user.self &&
                    Objects.equals(shortName, user.shortName) &&
                    Objects.equals(avatar, user.avatar);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, shortName, avatar, self);
        }
    }
}
