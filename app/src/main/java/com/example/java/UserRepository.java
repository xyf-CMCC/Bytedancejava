package com.example.java;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UserRepository {
    private final File storeFile;
    private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    public UserRepository(Context ctx) {
        this.storeFile = new File(ctx.getFilesDir(), "users.json");
    }

    public List<User> load(Context ctx) {
        if (!storeFile.exists()) return ensureInitialData(ctx);
        try {
            BufferedReader r = new BufferedReader(new FileReader(storeFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            JSONArray arr = new JSONArray(sb.toString());
            List<User> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(User.fromJson(arr.getJSONObject(i)));
            return list;
        } catch (IOException | JSONException e) {
            return ensureInitialData(ctx);
        }
    }

    public void saveAll(List<User> users) {
        try {
            JSONArray arr = new JSONArray();
            for (User u : users) arr.put(u.toJson());
            BufferedWriter w = new BufferedWriter(new FileWriter(storeFile, false));
            w.write(arr.toString());
            w.flush();
            w.close();
        } catch (IOException | JSONException ignored) {
        }
    }

    public void saveAllAsync(List<User> users) {
        executor.execute(() -> saveAll(users));
    }

    private List<User> ensureInitialData(Context ctx) {
        List<User> seed = generateSeed(ctx.getResources());
        saveAll(seed);
        return seed;
    }

    private List<User> generateSeed(Resources res) {
        List<User> list = new ArrayList<>();
        String[] names = new String[]{"王一", "李二", "张三", "赵四", "周五", "吴六", "郑七", "冯八", "陈九", "褚十", "sunny", "momo", "kira", "nova", "zero", "alpha", "beta", "gamma"};
        int[] avatars = new int[]{res.getIdentifier("ic_launcher", "mipmap", "com.example.java"), res.getIdentifier("ic_launcher_round", "mipmap", "com.example.java")};
        Random rnd = new Random(7);
        long base = System.currentTimeMillis() - 86400000L * 30;
        for (int i = 0; i < 50; i++) {
            long id = i + 1;
            String name = names[i % names.length] + i;
            String remark = i % 7 == 0 ? "同事" : "";
            boolean special = i % 9 == 0;
            int avatar = avatars[i % avatars.length];
            long followTime = base + (long) rnd.nextInt(30 * 24 * 60 * 60) * 1000L;
            list.add(new User(id, name, remark, special, avatar, followTime));
        }
        Collections.shuffle(list, rnd);
        SystemClock.sleep(50);
        return list;
    }
}
