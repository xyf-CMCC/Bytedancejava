package com.example.java.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users ORDER BY orderIndex ASC")
    List<UserEntity> getAllUsers();

    @Query("SELECT * FROM users ORDER BY orderIndex ASC LIMIT :limit OFFSET :offset")
    List<UserEntity> getUsersPaged(int limit, int offset);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserEntity> users);

    @Update
    void update(UserEntity user);

    @Query("UPDATE users SET followed = :followed WHERE id = :id")
    void updateFollowStatus(long id, boolean followed);

    @Query("UPDATE users SET special = :special WHERE id = :id")
    void updateSpecialStatus(long id, boolean special);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity findById(long id);

    @Query("DELETE FROM users")
    void clearAll();
    
    @Query("SELECT COUNT(*) FROM users")
    int getCount();
}
