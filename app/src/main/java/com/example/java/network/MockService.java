package com.example.java.network;

import com.example.java.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MockService {
    private static final int TOTAL_USERS = 1000;
    private final List<User> allUsers;

    public MockService() {
        allUsers = generateUsers();
    }

    private List<User> generateUsers() {
        List<User> list = new ArrayList<>();
        Random rnd = new Random(42); // Fixed seed for reproducibility
        long baseTime = System.currentTimeMillis();

        String[] names = new String[]{"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy", "Mallory", "Niaj", "Olivia", "Peggy", "Rupert", "Sybil", "Trent", "Victor", "Walter"};
        // Unique avatar per user using robohash (stable, diverse)

        for (int i = 0; i < TOTAL_USERS; i++) {
            long id = i + 1;
            String name = names[i % names.length] + " " + (i + 1);
            String remark = i % 10 == 0 ? "Friend" : "";
            boolean special = i % 20 == 0;
            // Unique avatar per id
            String avatarUrl = "https://picsum.photos/id/" + (id % 1000) + "/80/80.webp";
            long followTime = baseTime - (long) rnd.nextInt(365 * 24 * 60 * 60) * 1000L;
            
            list.add(new User(id, name, remark, special, true, 0, avatarUrl, followTime));
        }
        
        // Sort by follow time desc initially to simulate server order
        Collections.sort(list, (a, b) -> Long.compare(b.getFollowTime(), a.getFollowTime()));
        return list;
    }

    /**
     * Simulate network request with pagination.
     * @param offset 0-based start index
     * @param limit number of items to fetch
     * @return List of users, or empty list if out of bounds.
     */
    public List<User> getUsers(int offset, int limit) {
        // Simulate network delay
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }

        if (offset < 0 || limit < 1) return Collections.emptyList();

        if (offset >= allUsers.size()) return Collections.emptyList();

        int toIndex = Math.min(offset + limit, allUsers.size());
        return new ArrayList<>(allUsers.subList(offset, toIndex));
    }

    public void updateUserState(long id, Boolean special, Boolean followed, String remark) {
        for (int i = 0; i < allUsers.size(); i++) {
            User u = allUsers.get(i);
            if (u.getId() == id) {
                if (special != null) u.setSpecial(special);
                if (followed != null) u.setFollowed(followed);
                if (remark != null) u.setRemark(remark);
                break;
            }
        }
    }
}
