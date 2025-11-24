package com.example.java;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FollowListFragment extends Fragment {
    private UserRepository repo;
    private UsersAdapter adapter;
    private java.util.List<User> all;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_follow_list, container, false);
        repo = new UserRepository(requireContext());
        all = repo.load(requireContext());
        adapter = new UsersAdapter(new UsersAdapter.Listener() {
            @Override public void onToggleSpecial(User u, int position) { repo.saveAllAsync(all); adapter.notifyItemChanged(position); }
            @Override public void onToggleFollow(User u, int position) { repo.saveAllAsync(all); updateCount(v); adapter.notifyItemChanged(position); }
            @Override public void onEditRemark(User u, int position) { repo.saveAllAsync(all); updateCount(v); adapter.notifyItemChanged(position); }
            @Override public void onUnfollow(User u, int position) { u.setFollowed(false); repo.saveAllAsync(all); updateCount(v); adapter.notifyItemChanged(position); }
        });
        RecyclerView rv = v.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = v.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            all = repo.load(requireContext());
            updateCount(v);
            applyFilter(v);
            swipe.setRefreshing(false);
        });
        updateCount(v);
        applyFilter(v);
        return v;
    }

    private void applyFilter(View v) {
        java.util.List<User> filtered = new java.util.ArrayList<>();
        for (User u : all) {
            filtered.add(u);
        }
        java.util.Collections.sort(filtered, (a, b) -> Long.compare(b.getFollowTime(), a.getFollowTime()));
        adapter.submitList(filtered);
    }

    private void showRemarkDialog(User u, View root) {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setText(u.getRemark());
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("设置备注")
                .setView(et)
                .setPositiveButton("保存", (d, w) -> { u.setRemark(et.getText().toString().trim()); repo.saveAllAsync(all); updateCount(root); applyFilter(root); })
                .setNegativeButton("取消", (d, w) -> {})
                .show();
    }

    private void updateCount(View v) {
        android.widget.TextView tv = v.findViewById(R.id.my_follow_count);
        int cnt = 0; for (User u : all) if (u.isFollowed()) cnt++;
        tv.setText("我的关注（" + cnt + "人）");
    }
}
