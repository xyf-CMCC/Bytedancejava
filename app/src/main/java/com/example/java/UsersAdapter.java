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

import java.util.function.Consumer;

public class UsersAdapter extends ListAdapter<User, UsersAdapter.VH> {
    public interface Listener {
        void onToggleSpecial(User u, int position);
        void onToggleFollow(User u, int position);
        void onEditRemark(User u, int position);
        void onUnfollow(User u, int position);
    }

    private final Listener listener;

    public UsersAdapter(Listener l) {
        super(DIFF);
        setHasStableIds(true);
        this.listener = l;
    }

    @Override public long getItemId(int position) { return getItem(position).getId(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = getItem(pos);
        h.avatar.setImageResource(u.getAvatarRes());
        String displayName = u.getRemark().isEmpty() ? u.getName() : u.getRemark();
        h.name.setText(displayName);
        TextView chip = h.itemView.findViewById(R.id.chip_special);
        chip.setVisibility(u.isSpecial() ? View.VISIBLE : View.GONE);
        applyFollowStyle(h.status, u.isFollowed());
        h.status.setOnClickListener(v -> {
            boolean nf = !u.isFollowed();
            u.setFollowed(nf);
            applyFollowStyle(h.status, nf);
            listener.onToggleFollow(u, h.getBindingAdapterPosition());
        });
        h.avatar.setOnClickListener(v -> android.widget.Toast.makeText(v.getContext(), "已选中（" + displayName + "）", android.widget.Toast.LENGTH_SHORT).show());
        h.more.setOnClickListener(v -> showMenu(v, u, h.getBindingAdapterPosition()));
    }

    private void showMenu(View anchor, User u, int position) {
        BottomSheetDialog d = new BottomSheetDialog(anchor.getContext());
        View sheet = LayoutInflater.from(anchor.getContext()).inflate(R.layout.dialog_user_actions, null);
        d.setContentView(sheet);
        TextView title = sheet.findViewById(R.id.title);
        TextView subtitleName = sheet.findViewById(R.id.subtitle_name);
        TextView subtitleId = sheet.findViewById(R.id.subtitle_id);
        if (u.getRemark().isEmpty()) {
            title.setText(u.getName());
            subtitleName.setVisibility(View.GONE);
        } else {
            title.setText(u.getRemark());
            subtitleName.setVisibility(View.VISIBLE);
            subtitleName.setText("名字：" + u.getName());
        }
        SwitchMaterial sw = sheet.findViewById(R.id.switch_special);
        sw.setChecked(u.isSpecial());
        sw.setOnCheckedChangeListener((btn, checked) -> {
            u.setSpecial(checked);
            listener.onToggleSpecial(u, position);
        });
        sheet.findViewById(R.id.row_remark).setOnClickListener(v -> {
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
        sheet.findViewById(R.id.btn_unfollow).setOnClickListener(v -> { d.dismiss(); listener.onUnfollow(u, position); });
        sheet.findViewById(R.id.btn_close).setOnClickListener(v -> d.dismiss());
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
            return a.getName().equals(b.getName()) && a.getRemark().equals(b.getRemark()) && a.isSpecial() == b.isSpecial() && a.isFollowed() == b.isFollowed() && a.getAvatarRes() == b.getAvatarRes() && a.getFollowTime() == b.getFollowTime();
        }
    };

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView status;
        ImageButton more;
        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            name = v.findViewById(R.id.name);
            status = v.findViewById(R.id.status);
            more = v.findViewById(R.id.more);
        }
    }
}
