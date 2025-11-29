package com.example.java;

import android.content.Context;
import androidx.room.Room;

import com.example.java.db.AppDatabase;
import com.example.java.db.UserDao;
import com.example.java.db.UserEntity;
import com.example.java.network.MockService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UserRepository {
    private static UserRepository INSTANCE;
    private final UserDao userDao;
    private final MockService mockService;
    private final ExecutorService executor;

    private UserRepository(Context context) {
        AppDatabase db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "app_db")
                .fallbackToDestructiveMigration()
                .build();
        this.userDao = db.userDao();
        this.mockService = new MockService();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized UserRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UserRepository(context);
        }
        return INSTANCE;
    }

    public void loadMoreUsers(int currentCount, int limit, Consumer<Boolean> callback) {
        executor.execute(() -> {
            try {
                List<User> networkUsers = mockService.getUsers(currentCount, limit);
                if (!networkUsers.isEmpty()) {
                    List<UserEntity> entities = new ArrayList<>();
                    for (int i = 0; i < networkUsers.size(); i++) {
                        User u = networkUsers.get(i);
                        int orderIndex = currentCount + i;
                        com.example.java.db.UserEntity existing = userDao.findById(u.getId());
                        boolean special = existing != null ? existing.special : u.isSpecial();
                        boolean followed = existing != null ? existing.followed : u.isFollowed();
                        String remark = existing != null ? existing.remark : u.getRemark();
                        User merged = new User(u.getId(), u.getName(), remark, special, followed, u.getAvatarRes(), u.getAvatarUrl(), u.getFollowTime());
                        entities.add(toEntity(merged, orderIndex));
                    }
                    userDao.insertAll(entities);
                }
                if (callback != null) callback.accept(!networkUsers.isEmpty());
            } catch (Exception e) {
                if (callback != null) callback.accept(false);
            }
        });
    }

    public void refreshUsers(int limit, Consumer<Boolean> callback) {
        executor.execute(() -> {
            try {
                List<com.example.java.db.UserEntity> existingAll = userDao.getAllUsers();
                java.util.HashMap<Long, com.example.java.db.UserEntity> map = new java.util.HashMap<>();
                for (com.example.java.db.UserEntity e : existingAll) map.put(e.id, e);
                List<User> networkUsers = mockService.getUsers(0, limit);
                if (!networkUsers.isEmpty()) {
                    List<UserEntity> entities = new ArrayList<>();
                    for (int i = 0; i < networkUsers.size(); i++) {
                        User u = networkUsers.get(i);
                        com.example.java.db.UserEntity existing = map.get(u.getId());
                        boolean special = existing != null ? existing.special : u.isSpecial();
                        boolean followed = existing != null ? existing.followed : u.isFollowed();
                        String remark = existing != null ? existing.remark : u.getRemark();
                        User merged = new User(u.getId(), u.getName(), remark, special, followed, u.getAvatarRes(), u.getAvatarUrl(), u.getFollowTime());
                        entities.add(toEntity(merged, i));
                    }
                    userDao.clearAll();
                    userDao.insertAll(entities);
                }
                if (callback != null) callback.accept(!networkUsers.isEmpty());
            } catch (Exception e) {
                if (callback != null) callback.accept(false);
            }
        });
    }

    public void syncLoadedUsers(int currentCount, Consumer<Boolean> callback) {
        executor.execute(() -> {
            try {
                int pageSize = 10;
                List<UserEntity> toUpdate = new ArrayList<>();
                for (int offset = 0; offset < currentCount; offset += pageSize) {
                    int size = Math.min(pageSize, currentCount - offset);
                    List<User> networkUsers = mockService.getUsers(offset, size);
                    for (int i = 0; i < networkUsers.size(); i++) {
                        User u = networkUsers.get(i);
                        com.example.java.db.UserEntity existing = userDao.findById(u.getId());
                        int orderIndex = existing != null ? existing.orderIndex : offset + i;
                        boolean special = existing != null ? existing.special : u.isSpecial();
                        boolean followed = existing != null ? existing.followed : u.isFollowed();
                        String remark = existing != null ? existing.remark : u.getRemark();
                        User merged = new User(u.getId(), u.getName(), remark, special, followed, u.getAvatarRes(), u.getAvatarUrl(), u.getFollowTime());
                        toUpdate.add(toEntity(merged, orderIndex));
                    }
                }
                if (!toUpdate.isEmpty()) userDao.insertAll(toUpdate);
                if (callback != null) callback.accept(true);
            } catch (Exception e) {
                if (callback != null) callback.accept(false);
            }
        });
    }

    public void getAllUsers(Consumer<List<User>> callback) {
        executor.execute(() -> {
            List<UserEntity> entities = userDao.getAllUsers();
            List<User> users = new ArrayList<>();
            for (UserEntity e : entities) {
                users.add(toUser(e));
            }
            callback.accept(users);
        });
    }

    public void updateFollowStatus(User user) {
        executor.execute(() -> {
            userDao.updateFollowStatus(user.getId(), user.isFollowed());
            mockService.updateUserState(user.getId(), null, user.isFollowed(), null);
        });
    }
    
    public void updateUser(User user) {
        executor.execute(() -> {
            com.example.java.db.UserEntity existing = userDao.findById(user.getId());
            int orderIndex = existing != null ? existing.orderIndex : 0;
            userDao.update(toEntity(user, orderIndex));
            mockService.updateUserState(user.getId(), user.isSpecial(), user.isFollowed(), user.getRemark());
        });
    }

    public void updateFollowStatus(User user, Runnable onDone) {
        executor.execute(() -> {
            userDao.updateFollowStatus(user.getId(), user.isFollowed());
            mockService.updateUserState(user.getId(), null, user.isFollowed(), null);
            if (onDone != null) onDone.run();
        });
    }

    public void updateUser(User user, Runnable onDone) {
        executor.execute(() -> {
            com.example.java.db.UserEntity existing = userDao.findById(user.getId());
            int orderIndex = existing != null ? existing.orderIndex : 0;
            userDao.update(toEntity(user, orderIndex));
            mockService.updateUserState(user.getId(), user.isSpecial(), user.isFollowed(), user.getRemark());
            if (onDone != null) onDone.run();
        });
    }

    public void updateSpecialStatus(User user, Runnable onDone) {
        executor.execute(() -> {
            userDao.updateSpecialStatus(user.getId(), user.isSpecial());
            mockService.updateUserState(user.getId(), user.isSpecial(), null, null);
            if (onDone != null) onDone.run();
        });
    }

    private UserEntity toEntity(User u, int orderIndex) {
        return new UserEntity(u.getId(), u.getName(), u.getRemark(), u.isSpecial(), u.isFollowed(), u.getAvatarRes(), u.getAvatarUrl(), u.getFollowTime(), orderIndex);
    }

    private User toUser(UserEntity e) {
        return new User(e.id, e.name, e.remark, e.special, e.followed, e.avatarRes, e.avatarUrl, e.followTime);
    }
}
