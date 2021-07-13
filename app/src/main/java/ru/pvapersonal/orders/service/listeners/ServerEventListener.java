package ru.pvapersonal.orders.service.listeners;

import org.json.JSONObject;

public abstract class ServerEventListener {
    public abstract void eventExecuted(int code, String response);
}
