package com.example.java;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

public class UserViewModel extends AndroidViewModel {
    private final UserRepository repository;
    private final MutableLiveData<List<User>> users = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 10;
    private boolean isLastPage = false;

    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = UserRepository.getInstance(application);
    }

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void refresh() {
        loading.postValue(true);
        int currentCount = users.getValue() != null ? users.getValue().size() : 0;
        if (currentCount == 0) {
            repository.loadMoreUsers(0, PAGE_SIZE, success -> {
                reloadFromDb();
            });
        } else {
            repository.syncLoadedUsers(currentCount, success -> {
                reloadFromDb();
            });
        }
    }

    public void loadNextPage() {
        if (loading.getValue() == Boolean.TRUE || isLastPage) return;
        loading.postValue(true);
        int offset = currentOffset;
        repository.loadMoreUsers(offset, PAGE_SIZE, success -> {
            if (!success) {
                isLastPage = true;
            } else {
                currentOffset = offset + PAGE_SIZE;
            }
            reloadFromDb();
        });
    }

    private void reloadFromDb() {
        repository.getAllUsers(data -> {
            users.postValue(data);
            currentOffset = data != null ? data.size() : 0;
            loading.postValue(false);
        });
    }
    
    public void updateUser(User user) {
        repository.updateUser(user, this::reloadFromDb);
    }
    
    public void toggleFollow(User user) {
        user.setFollowed(!user.isFollowed());
        repository.updateFollowStatus(user, this::reloadFromDb);
    }
}
