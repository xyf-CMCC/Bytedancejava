package com.example.java.network;

import com.example.java.User;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class MockServiceTest {
    @Test
    public void testPagination() {
        MockService service = new MockService();
        
        // Test Offset 0 (First Page)
        List<User> page1 = service.getUsers(0, 10);
        assertEquals(10, page1.size());
        
        // Test Offset 10 (Second Page)
        List<User> page2 = service.getUsers(10, 10);
        assertEquals(10, page2.size());
        assertNotEquals(page1.get(0).getId(), page2.get(0).getId());

        // Test Out of bounds
        List<User> pageOut = service.getUsers(2000, 10); // 2000 > 1000
        assertEquals(0, pageOut.size());
        
        // Test Last Page (exact)
        List<User> lastPage = service.getUsers(990, 10);
        assertEquals(10, lastPage.size());
    }
}
