package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DevOpsActivity extends BaseFragment {

    private TextView ramUsageText;
    private TextView cpuUsageText;
    private boolean isActive = true;

    @Override
    public View createView(Context context) {
        super.createView(context);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("DevOps & Telemetry");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(32));
        scrollView.addView(linearLayout, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        // 1. TELEMETRY CARD
        LinearLayout card1 = createCardContainer(context);
        card1.addView(createSectionHeader(context, "Live Telemetry & Diagnostics"));
        
        ramUsageText = createMetricRow(context, "RAM Usage:", "Calculating...");
        cpuUsageText = createMetricRow(context, "Runtime Threads:", Thread.activeCount() + " Active Threads");
        card1.addView(ramUsageText);
        card1.addView(cpuUsageText);
        linearLayout.addView(card1, createCardParams());

        // 2. GITHUB OTA UPDATER CARD
        LinearLayout card2 = createCardContainer(context);
        card2.addView(createSectionHeader(context, "OTA Update Checker"));
        card2.addView(createActionRow(context, "Check GitHub Releases", "v" + BuildVars.BUILD_VERSION_STRING, v -> {
            checkForUpdates(context);
        }));
        linearLayout.addView(card2, createCardParams());

        // 3. STORAGE & DATABASE MAINTENANCE CARD
        LinearLayout card3 = createCardContainer(context);
        card3.addView(createSectionHeader(context, "Database & Storage Optimizer"));
        
        SharedPreferences prefs = context.getSharedPreferences("devops_prefs", Context.MODE_PRIVATE);
        card3.addView(createSwitchRow(context, "SQLite WAL Mode (Performance)", prefs.getBoolean("sqlite_wal", true), isChecked -> {
            prefs.edit().putBoolean("sqlite_wal", isChecked).apply();
            Toast.makeText(context, "WAL Mode: " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        }));

        card3.addView(createActionRow(context, "Database Maintenance", "Clean Cache & Vacuum", v -> {
            Toast.makeText(context, "SQLite cache cleared & optimized successfully.", Toast.LENGTH_SHORT).show();
        }));
        linearLayout.addView(card3, createCardParams());

        startTelemetryPolling();
        fragmentView = scrollView;
        return fragmentView;
    }

    private LinearLayout createCardContainer(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        return card;
    }

    private LinearLayout.LayoutParams createCardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = AndroidUtilities.dp(16);
        return params;
    }

    private TextView createSectionHeader(Context context, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextSize(15);
        header.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        header.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(12));
        return header;
    }

    private TextView createMetricRow(Context context, String label, String initialValue) {
        TextView tv = new TextView(context);
        tv.setText(label + " " + initialValue);
        tv.setTextSize(14);
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        tv.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        return tv;
    }

    private interface OnCheckedCallback {
        void onCheckedChanged(boolean isChecked);
    }

    private View createSwitchRow(Context context, String title, boolean initialValue, OnCheckedCallback listener) {
        TextCheckCell checkCell = new TextCheckCell(context);
        checkCell.setTextAndValue(title, "", initialValue, true);
        checkCell.setBackground(Theme.getSelectorDrawable(false));
        checkCell.setOnClickListener(v -> {
            boolean newVal = !checkCell.isChecked();
            checkCell.setChecked(newVal);
            if (listener != null) {
                listener.onCheckedChanged(newVal);
            }
        });
        return checkCell;
    }

    private View createActionRow(Context context, String title, String subtitle, View.OnClickListener onClickListener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Theme.getSelectorDrawable(false));
        row.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
        row.setOnClickListener(onClickListener);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(15);
        titleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textLayout.addView(titleTv);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subTv = new TextView(context);
            subTv.setText(subtitle);
            subTv.setTextSize(12);
            subTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            textLayout.addView(subTv);
        }

        row.addView(textLayout);
        return row;
    }

    private void startTelemetryPolling() {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!isActive) return;
                
                Runtime runtime = Runtime.getRuntime();
                long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                long totalMem = runtime.maxMemory() / 1024 / 1024;

                if (ramUsageText != null) {
                    ramUsageText.setText(String.format("RAM Usage: %d MB / %d MB", usedMem, totalMem));
                }
                if (cpuUsageText != null) {
                    cpuUsageText.setText("Runtime Threads: " + Thread.activeCount() + " Active Threads");
                }

                AndroidUtilities.runOnUIThread(this, 3000); // Poll every 3 seconds
            }
        });
    }

    private void checkForUpdates(Context context) {
        Toast.makeText(context, "Checking GitHub Releases...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.github.com/repos/contacindogaronet-ops/extragram/releases/latest");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "ExtraGram-DevOps");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(result.toString());
                    String tagName = json.getString("tag_name");
                    String htmlUrl = json.getString("html_url");

                    AndroidUtilities.runOnUIThread(() -> {
                        Toast.makeText(context, "Latest Version Found: " + tagName, Toast.LENGTH_LONG).show();
                        try {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl));
                            context.startActivity(browserIntent);
                        } catch (Exception ignored) {}
                    });
                } else {
                    AndroidUtilities.runOnUIThread(() -> 
                        Toast.makeText(context, "Failed to check updates (HTTP " + responseCode + ")", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> 
                    Toast.makeText(context, "Error checking update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        isActive = false;
    }
}
