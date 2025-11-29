package com.example.java;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.example.java.glide.AvatarPreloadProvider;
import com.example.java.databinding.FragmentFollowListBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class FollowListFragment extends Fragment {
    private UserViewModel viewModel;
    private UsersAdapter adapter;
    private FragmentFollowListBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFollowListBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(UserViewModel.class);
        
        adapter = new UsersAdapter(new UsersAdapter.Listener() {
            @Override public void onToggleSpecial(User u, int position) { viewModel.updateUser(u); adapter.notifyItemChanged(position); }
            @Override public void onToggleFollow(User u, int position) { viewModel.updateUser(u); adapter.notifyItemChanged(position); }
            @Override public void onEditRemark(User u, int position) { viewModel.updateUser(u); adapter.notifyItemChanged(position); }
            @Override public void onUnfollow(User u, int position) { viewModel.toggleFollow(u); adapter.notifyItemChanged(position); }
        });

        RecyclerView rv = binding.list;
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setItemPrefetchEnabled(true);
        lm.setInitialPrefetchItemCount(10);
        rv.setLayoutManager(lm);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setItemAnimator(null);
        RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
        rv.setRecycledViewPool(pool);
        pool.setMaxRecycledViews(0, 20);
        rv.setAdapter(adapter);
        
        // Scroll Listener for Pagination
        FixedPreloadSizeProvider<String> sizeProvider = new FixedPreloadSizeProvider<>(80, 80);
        AvatarPreloadProvider provider = new AvatarPreloadProvider(Glide.with(this), adapter);
        RecyclerViewPreloader<String> preloader = new RecyclerViewPreloader<>(Glide.with(this), provider, sizeProvider, 10);
        rv.addOnScrollListener(preloader);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int lastVisible = lm.findLastVisibleItemPosition();
                int total = adapter.getItemCount();
                int threshold = 2;
                boolean isLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
                if (!isLoading && total > 0 && lastVisible >= total - 1 - threshold) {
                    viewModel.loadNextPage();
                }
            }
        });

        SwipeRefreshLayout swipe = binding.swipe;
        swipe.setOnRefreshListener(() -> viewModel.refresh());

        TextView tv = binding.myFollowCount;

        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            adapter.submitList(users);
            int cnt = 0;
            if (users != null) {
                for (User u : users) if (u.isFollowed()) cnt++;
            }
            tv.setText("我的关注（" + cnt + "人）");
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (swipe.isRefreshing() && !loading) swipe.setRefreshing(false);
        });

        // Initial Load
        viewModel.refresh();
        
        return binding.getRoot();
    }
}
