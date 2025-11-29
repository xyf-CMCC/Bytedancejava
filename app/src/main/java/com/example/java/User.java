package com.example.java;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private long id;
    private String name;
    private String remark;
    private boolean special;
    private boolean followed;
    private int avatarRes;
    private String avatarUrl;
    private long followTime;

    public User(long id, String name, String remark, boolean special, int avatarRes, long followTime) {
        this(id, name, remark, special, true, avatarRes, null, followTime);
    }

    public User(long id, String name, String remark, boolean special, boolean followed, int avatarRes, long followTime) {
        this(id, name, remark, special, followed, avatarRes, null, followTime);
    }

    public User(long id, String name, String remark, boolean special, boolean followed, int avatarRes, String avatarUrl, long followTime) {
        this.id = id;
        this.name = name;
        this.remark = remark;
        this.special = special;
        this.followed = followed;
        this.avatarRes = avatarRes;
        this.avatarUrl = avatarUrl;
        this.followTime = followTime;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getRemark() { return remark; }
    public boolean isSpecial() { return special; }
    public boolean isFollowed() { return followed; }
    public int getAvatarRes() { return avatarRes; }
    public String getAvatarUrl() { return avatarUrl; }
    public long getFollowTime() { return followTime; }

    public void setRemark(String remark) { this.remark = remark; }
    public void setSpecial(boolean special) { this.special = special; }
    public void setFollowed(boolean followed) { this.followed = followed; }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("remark", remark);
        o.put("special", special);
        o.put("followed", followed);
        o.put("avatarRes", avatarRes);
        o.put("avatarUrl", avatarUrl);
        o.put("followTime", followTime);
        return o;
    }

    public static User fromJson(JSONObject o) throws JSONException {
        long id = o.getLong("id");
        String name = o.getString("name");
        String remark = o.optString("remark", "");
        boolean special = o.optBoolean("special", false);
        boolean followed = o.optBoolean("followed", true);
        int avatarRes = o.optInt("avatarRes", 0);
        String avatarUrl = o.optString("avatarUrl", null);
        long followTime = o.optLong("followTime", System.currentTimeMillis());
        return new User(id, name, remark, special, followed, avatarRes, avatarUrl, followTime);
    }
}
