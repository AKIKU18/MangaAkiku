package com.example.mangav5.UI;

import android.app.Activity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.mangav5.Database.AppDatabase;
import com.example.mangav5.Entity.SourceEntity;
import com.example.mangav5.R;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SourceDrawerHandler {
    private final Activity activity;
    private final DrawerLayout drawerLayout;
    private final OnSourceSelectedListener listener;
    
    private final Map<Button, String> sourceButtons = new HashMap<>();
    private final Map<ImageButton, String> mainSourceButtons = new HashMap<>();
    private final Map<String, Integer> glowMap = new HashMap<>();

    public interface OnSourceSelectedListener {
        void onSourceSelected(String sourceName);
    }

    public SourceDrawerHandler(Activity activity, DrawerLayout drawerLayout, OnSourceSelectedListener listener) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;
        this.listener = listener;
    }

    /**
     * Registers all UI components for a source in one go.
     */
    public void addSource(String name, int buttonId, int mainButtonId, int glowId) {
        Button btn = activity.findViewById(buttonId);
        ImageButton mainBtn = activity.findViewById(mainButtonId);
        
        if (btn != null) sourceButtons.put(btn, name);
        if (mainBtn != null) mainSourceButtons.put(mainBtn, name);
        glowMap.put(name, glowId);
    }

    public void setup() {
        ImageButton menuButton = activity.findViewById(R.id.button_menu);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        for (Map.Entry<Button, String> entry : sourceButtons.entrySet()) {
            entry.getKey().setOnClickListener(v -> {
                listener.onSourceSelected(entry.getValue());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        for (Map.Entry<ImageButton, String> entry : mainSourceButtons.entrySet()) {
            entry.getKey().setOnClickListener(v -> {
                String sourceToSetAsMain = entry.getValue();
                updateMainSourceIcon(sourceToSetAsMain);

                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(activity);
                    SourceEntity source = new SourceEntity(entry.getValue());
                    db.sourceDao().addSource(source);
                });

                Toast.makeText(activity, sourceToSetAsMain + " set as main source.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    public void updateMainSourceIcon(String activeSource) {
        for (Map.Entry<ImageButton, String> entry : mainSourceButtons.entrySet()) {
            entry.getKey().setImageResource(entry.getValue().equals(activeSource) ? R.drawable.ic_sharingan_light : R.drawable.ic_sharingan_dark);
        }
    }

    public void updateSourceGlow(String serviceFeed) {
        for (int id : glowMap.values()) {
            View glow = activity.findViewById(id);
            if (glow != null) {
                glow.clearAnimation();
                glow.setVisibility(View.GONE);
            }
        }

        Integer glowId = glowMap.get(serviceFeed);
        if (glowId == null) return;

        View targetGlow = activity.findViewById(glowId);
        if (targetGlow != null) {
            targetGlow.setVisibility(View.VISIBLE);
            AlphaAnimation pulse = new AlphaAnimation(0.3f, 1f);
            pulse.setDuration(1000);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            targetGlow.startAnimation(pulse);
        }
    }
}
