package ru.pvapersonal.orders.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FullUsersModel {
    public FullUserItem admin;
    public int id;
    public Long qDate;
    public List<FullUserItem> items;

    public FullUsersModel(JSONObject res) throws JSONException {
        admin = new FullUserItem(res.getJSONObject("admin"));
        JSONArray array = res.getJSONArray("users");
        items = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            items.add(new FullUserItem(array.getJSONObject(i)));
        }
        qDate = res.getLong("queryDate");
        id = res.getInt("id");
    }

    public void addUser(JSONObject newUser, boolean self) throws JSONException {
        items.add(new FullUserItem(newUser, self));
    }

    public void removeUser(int toDelete) throws JSONException {
        List<FullUserItem> newArray = new ArrayList<>();
        for (FullUserItem f : items) {
            if (f.userId != toDelete) {
                newArray.add(f);
            }
        }
        items = newArray;
    }
}
