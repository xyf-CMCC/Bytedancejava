package com.example.java.db;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UserDaoTest {
    private AppDatabase db;
    private UserDao dao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        dao = db.userDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void writeUserAndReadInList() {
        UserEntity u1 = new UserEntity(1, "Alice", "", false, true, 0, "url1", 1000);
        UserEntity u2 = new UserEntity(2, "Bob", "", false, true, 0, "url2", 2000);
        
        dao.insertAll(Arrays.asList(u1, u2));
        
        List<UserEntity> all = dao.getAllUsers();
        assertEquals(2, all.size());
        // Ordered by followTime DESC
        assertEquals(2, all.get(0).id);
        assertEquals(1, all.get(1).id);
    }
    
    @Test
    public void testPaging() {
        UserEntity u1 = new UserEntity(1, "Alice", "", false, true, 0, "url1", 1000);
        UserEntity u2 = new UserEntity(2, "Bob", "", false, true, 0, "url2", 2000);
        UserEntity u3 = new UserEntity(3, "Charlie", "", false, true, 0, "url3", 3000);
        
        dao.insertAll(Arrays.asList(u1, u2, u3));
        
        // DESC order: Charlie(3), Bob(2), Alice(1)
        List<UserEntity> page1 = dao.getUsersPaged(2, 0);
        assertEquals(2, page1.size());
        assertEquals(3, page1.get(0).id);
        assertEquals(2, page1.get(1).id);
        
        List<UserEntity> page2 = dao.getUsersPaged(2, 2);
        assertEquals(1, page2.size());
        assertEquals(1, page2.get(0).id);
    }
}
