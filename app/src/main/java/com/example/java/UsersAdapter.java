package com.example.java;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.java.databinding.ItemUserBinding;
import com.example.java.databinding.DialogUserActionsBinding;

import java.util.function.Consumer;

public class UsersAdapter extends ListAdapter<User, UsersAdapter.VH> {
    public interface Listener {
        void onToggleSpecial(User u, int position);
        void onToggleFollow(User u, int position);
        void onEditRemark(User u, int position);
        void onUnfollow(User u, int position);
    }

    private final Listener listener;

    private final StringBuilder sb = new StringBuilder(64);

    public UsersAdapter(Listener l) {
        super(DIFF);
        setHasStableIds(true);
        this.listener = l;
    }

    public User getItemAt(int position) {
        return getItem(position);
    }

    @Override public long getItemId(int position) { return getItem(position).getId(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding b = ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = getItem(pos);
        if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) {
            String fallback = "https://robohash.org/" + u.getId() + ".png?size=80x80&set=set3";
            com.bumptech.glide.Glide.with(h.binding.avatar)
                    .load(u.getAvatarUrl())
                    .override(80, 80)
                    .thumbnail(0.25f)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(80))
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                    .circleCrop()
                    .error(
                            com.bumptech.glide.Glide.with(h.binding.avatar)
                                    .load(fallback)
                                    .override(80, 80)
                                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                                    .circleCrop()
                    )
                    .into(h.binding.avatar);
        } else {
            h.binding.avatar.setImageResource(u.getAvatarRes() != 0 ? u.getAvatarRes() : R.mipmap.ic_launcher_round);
        }
        sb.setLength(0);
        String displayName = u.getRemark().isEmpty() ? u.getName() : u.getRemark();
        h.binding.name.setText(displayName);
        h.binding.chipSpecial.setVisibility(u.isSpecial() ? View.VISIBLE : View.GONE);
        applyFollowStyle(h.binding.status, u.isFollowed());
        h.binding.status.setOnClickListener(v -> {
            boolean nf = !u.isFollowed();
            u.setFollowed(nf);
            applyFollowStyle(h.binding.status, nf);
            listener.onToggleFollow(u, h.getBindingAdapterPosition());
        });
        sb.append("已选中（").append(displayName).append("）");
        String msg = sb.toString();
        h.binding.avatar.setOnClickListener(v -> android.widget.Toast.makeText(v.getContext(), msg, android.widget.Toast.LENGTH_SHORT).show());
        h.binding.more.setOnClickListener(v -> showMenu(v, u, h.getBindingAdapterPosition()));
    }

    private void showMenu(View anchor, User u, int position) {
        BottomSheetDialog d = new BottomSheetDialog(anchor.getContext());
        DialogUserActionsBinding b = DialogUserActionsBinding.inflate(LayoutInflater.from(anchor.getContext()));
        d.setContentView(b.getRoot());
        TextView title = b.title;
        TextView subtitleName = b.subtitleName;
        TextView subtitleId = b.subtitleId;
        if (u.getRemark().isEmpty()) {
            title.setText(u.getName());
            subtitleName.setVisibility(View.GONE);
        } else {
            title.setText(u.getRemark());
            subtitleName.setVisibility(View.VISIBLE);
            subtitleName.setText("名字：" + u.getName());
        }
        SwitchMaterial sw = b.switchSpecial;
        sw.setChecked(u.isSpecial());
        sw.setOnCheckedChangeListener((btn, checked) -> {
            u.setSpecial(checked);
            listener.onToggleSpecial(u, position);
        });
        b.rowRemark.setOnClickListener(v -> {
            android.widget.EditText et = new android.widget.EditText(v.getContext());
            et.setText(u.getRemark());
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                    .setTitle("设置备注")
                    .setView(et)
                    .setPositiveButton("保存", (dlg, w) -> {
                        u.setRemark(et.getText().toString().trim());
                        if (u.getRemark().isEmpty()) {
                            title.setText(u.getName());
                            subtitleName.setVisibility(View.GONE);
                        } else {
                            title.setText(u.getRemark());
                            subtitleName.setVisibility(View.VISIBLE);
                            subtitleName.setText("名字：" + u.getName());
                        }
                        listener.onEditRemark(u, position);
                    })
                    .setNegativeButton("取消", (dlg, w) -> {})
                    .show();
        });
        b.btnUnfollow.setOnClickListener(v -> { d.dismiss(); listener.onUnfollow(u, position); });
        b.btnClose.setOnClickListener(v -> d.dismiss());
        d.show();
    }

    private void applyFollowStyle(TextView tv, boolean followed) {
        if (followed) {
            tv.setText("已关注");
            tv.setBackgroundResource(R.drawable.bg_follow_status);
            tv.setTextColor(android.graphics.Color.parseColor("#000000"));
        } else {
            tv.setText("关注");
            tv.setBackgroundResource(R.drawable.bg_follow_action);
            tv.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        }
    }

    static final DiffUtil.ItemCallback<User> DIFF = new DiffUtil.ItemCallback<User>() {
        @Override public boolean areItemsTheSame(@NonNull User a, @NonNull User b) { return a.getId() == b.getId(); }
        @Override public boolean areContentsTheSame(@NonNull User a, @NonNull User b) {
            String au = a.getAvatarUrl() == null ? "" : a.getAvatarUrl();
            String bu = b.getAvatarUrl() == null ? "" : b.getAvatarUrl();
            return a.getName().equals(b.getName()) && a.getRemark().equals(b.getRemark()) && a.isSpecial() == b.isSpecial() && a.isFollowed() == b.isFollowed() && a.getAvatarRes() == b.getAvatarRes() && a.getFollowTime() == b.getFollowTime() && au.equals(bu);
        }
    };

    static class VH extends RecyclerView.ViewHolder {
        final ItemUserBinding binding;
        VH(@NonNull ItemUserBinding b) {
            super(b.getRoot());
            this.binding = b;
        }
    }
}
