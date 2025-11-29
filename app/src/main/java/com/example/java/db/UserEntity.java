package com.example.java.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    public long id;
    public String name;
    public String remark;
    public boolean special;
    public boolean followed;
    public int avatarRes;
    public String avatarUrl;
    public long followTime;
    public int orderIndex;

    public UserEntity() {}

    @Ignore
    public UserEntity(long id, String name, String remark, boolean special, boolean followed, int avatarRes, String avatarUrl, long followTime, int orderIndex) {
        this.id = id;
        this.name = name;
        this.remark = remark;
        this.special = special;
        this.followed = followed;
        this.avatarRes = avatarRes;
        this.avatarUrl = avatarUrl;
        this.followTime = followTime;
        this.orderIndex = orderIndex;
    }

    @Ignore
    public UserEntity(long id, String name, String remark, boolean special, boolean followed, int avatarRes, String avatarUrl, long followTime) {
        this.id = id;
        this.name = name;
        this.remark = remark;
        this.special = special;
        this.followed = followed;
        this.avatarRes = avatarRes;
        this.avatarUrl = avatarUrl;
        this.followTime = followTime;
        this.orderIndex = 0;
    }
}
