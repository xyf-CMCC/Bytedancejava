package com.example.java;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PlaceholderFragment extends Fragment {
    public static PlaceholderFragment newInstance(String text) {
        PlaceholderFragment f = new PlaceholderFragment();
        Bundle b = new Bundle();
        b.putString("text", text);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_placeholder, container, false);
        String t = getArguments() != null ? getArguments().getString("text", "暂无内容") : "暂无内容";
        ((TextView) v.findViewById(R.id.placeholder_text)).setText(t);
        return v;
    }
}
