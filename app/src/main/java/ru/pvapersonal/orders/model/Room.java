package ru.pvapersonal.orders.model;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Room {
    public String name;
    public boolean isAdmin;
    public long creationDate;
    public int id;
    public boolean isLocked;
    public String creator;
    public int creatorId;
    public Status status;
    public Long start = null;
    public Long end = null;
    public int maxMembers;
    public int partType;

    @NonNull
    @NotNull
    @Override
    public String toString() {
        return String.format(new Locale("RU"), "%s %s %d %s %d", name, isAdmin ?
                        "Админ" : "Участник", id,  isLocked ? "Закрытая" : "Открытая", maxMembers);
    }

    public Room(JSONObject res) throws JSONException {
        //0 is not member; 1 is member; 2 is admin
        if(res.has("partitionType")) {
            partType = res.getInt("partitionType");
        }else{
            partType = 0;
        }
        name = res.getString("name");
        status = Status.valueOf(res.getString("status"));
        creatorId = res.getInt("creatorId");
        if(res.has("isAdmin")) {
            isAdmin = res.getBoolean("isAdmin");
        }else{
            isAdmin = true;
        }
        if(res.has("start")){
            start = res.getLong("start");
        }
        if(res.has("end")){
            end = res.getLong("end");
        }
        creationDate = res.getLong("creationDate");
        id = res.getInt("id");
        isLocked = res.getBoolean("passworded");
        creator = res.getString("creator");
        maxMembers = res.getInt("maxMembers");
    }

    public String getExecutionDate(){
        if(start == null || end == null){
            return null;
        }else{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm");
            return simpleDateFormat.format(new Date(start)) + " — " + simpleDateFormat.format(new Date(end));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return id == room.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isAdmin, creationDate, id, isLocked, creator, status, maxMembers);
    }

    public boolean matchesFilter(String keyWords){
        String describe = toString();
        for(String string : keyWords.split(" ")){
            if (describe.toLowerCase().contains(string)){
                return true;
            }
        }
        return false;
    }

    public boolean areContentsTheSame(Room room) {
        if (this == room) return true;
        if (room  == null || getClass() != room.getClass()) return false;
        return isAdmin == room.isAdmin &&
                id == room.id &&
                isLocked == room.isLocked &&
                maxMembers == room.maxMembers &&
                Objects.equals(name, room.name) &&
                Objects.equals(creator, room.creator) &&
                status == room.status;
    }
}
