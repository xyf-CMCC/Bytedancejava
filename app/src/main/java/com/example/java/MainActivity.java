package com.example.java;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private final String[] titles = new String[]{"互关", "关注", "粉丝", "朋友"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ViewPager2 pager = findViewById(R.id.pager);
        TabLayout tabs = findViewById(R.id.tabs);
        android.widget.ImageButton back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());
        pager.setAdapter(new FragmentStateAdapter(this) {
            @Override public androidx.fragment.app.Fragment createFragment(int position) {
                if (position == 1) return new FollowListFragment();
                if (position == 0) return PlaceholderFragment.newInstance("暂无互关");
                if (position == 2) return PlaceholderFragment.newInstance("暂无粉丝");
                return PlaceholderFragment.newInstance("暂无朋友");
            }
            @Override public int getItemCount() { return 4; }
        });
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(titles[pos])).attach();
    }
}
