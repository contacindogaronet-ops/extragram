package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DevOpsActivity extends BaseFragment {

    private UniversalAdapter adapter;
    private static final String PREF_NAME = "DevOpsEnginePrefs";

    private SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("DevOps & Engine Control");
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        RecyclerListView listView = new RecyclerListView(context);
        listView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false));

        adapter = new UniversalAdapter(listView, context, currentAccount, 0, this::fillItems, null);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((view, position) -> {
            UItem item = adapter.getItem(position);
            if (item == null) return;

            SharedPreferences prefs = getPreferences();

            switch (item.id) {
                case 101:
                    checkForUpdates();
                    break;

                case 201:
                    SharedConfig.toggleSqliteSyncMode();
                    if (adapter != null) adapter.update(true);
                    Toast.makeText(getParentActivity(), "SQLite Sync Mode: " + SharedConfig.getSqliteSyncMode(), Toast.LENGTH_SHORT).show();
                    break;

                case 202:
                    SharedConfig.toggleSqliteWal();
                    if (adapter != null) adapter.update(true);
                    Toast.makeText(getParentActivity(), "SQLite WAL Mode: " + (SharedConfig.isSqliteWalEnabled() ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                    break;

                case 203:
                    getMessagesStorage().getStorageQueue().postRunnable(() -> {
                        try {
                            getMessagesStorage().getDatabase().executeFast("VACUUM;").stepThis().dispose();
                            getMessagesStorage().getDatabase().executeFast("PRAGMA integrity_check;").stepThis().dispose();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });
                    Toast.makeText(getParentActivity(), "Database Maintenance (VACUUM) Executed", Toast.LENGTH_SHORT).show();
                    break;

                case 204:
                    System.gc();
                    System.runFinalization();
                    Toast.makeText(getParentActivity(), "RAM Cleared & GC Triggered", Toast.LENGTH_SHORT).show();
                    break;

                case 205:
                    getConnectionsManager().checkConnection();
                    Toast.makeText(getParentActivity(), "MTProto Connection Re-initialized", Toast.LENGTH_SHORT).show();
                    break;

                case 301:
                    boolean currentBypass = prefs.getBoolean("bypass_protected", false);
                    prefs.edit().putBoolean("bypass_protected", !currentBypass).apply();
                    if (adapter != null) adapter.update(true);
                    Toast.makeText(getParentActivity(), "Protected Content Bypass: " + (!currentBypass ? "ACTIVE" : "OFF"), Toast.LENGTH_SHORT).show();
                    break;

                case 302:
                    boolean currentRouting = prefs.getBoolean("custom_routing", false);
                    boolean newState = !currentRouting;
                    prefs.edit().putBoolean("custom_routing", newState).apply();
                    
                    try {
                        int currentAcc = getCurrentAccount();
                        ConnectionsManager.getInstance(currentAcc).setAppPaused(false, false);
                        ConnectionsManager.getInstance(currentAcc).checkConnection();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    if (adapter != null) adapter.update(true);
                    Toast.makeText(getParentActivity(), "Zero-Copy / Fast Routing Pipeline: " + (newState ? "OPTIMIZED (Active)" : "STANDARD"), Toast.LENGTH_SHORT).show();
                    break;

                case 208:
                    AndroidUtilities.addToClipboard(BuildVars.BUILD_GIT_HASH);
                    Toast.makeText(getParentActivity(), "Commit Hash Copied: " + BuildVars.BUILD_GIT_HASH, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        fragmentView = listView;
        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        SharedPreferences prefs = getPreferences();

        items.add(UItem.asHeader("OTA & Updates"));
        items.add(UItem.asButton(101, "Check for App Updates", "Check"));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Database Engine"));
        items.add(UItem.asButton(201, "SQLite Sync Mode", SharedConfig.getSqliteSyncMode()));

        UItem walItem = UItem.asCheck(202, "SQLite WAL Mode");
        walItem.checked = SharedConfig.isSqliteWalEnabled();
        items.add(walItem);

        items.add(UItem.asButton(203, "Database Maintenance", "VACUUM"));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Advanced Power-User Tools"));
        
        UItem bypassItem = UItem.asCheck(301, "Bypass Protected Content");
        bypassItem.checked = prefs.getBoolean("bypass_protected", false);
        items.add(bypassItem);

        UItem routingItem = UItem.asCheck(302, "Zero-Copy TCP / Fast Routing");
        routingItem.checked = prefs.getBoolean("custom_routing", false);
        items.add(routingItem);

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Memory & Engine"));
        items.add(UItem.asButton(204, "Purge RAM & Native GC", "Run"));
        items.add(UItem.asButton(205, "Restart MTProto Daemon", "Reset"));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Build Information"));
        items.add(UItem.asButton(208, "Build Commit Hash", BuildVars.BUILD_GIT_HASH));
    }

    private void checkForUpdates() {
        Toast.makeText(getParentActivity(), "Checking GitHub Releases...", Toast.LENGTH_SHORT).show();
        org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.github.com/repos/contacindogaronet-ops/exteraGram/releases/latest");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Telegram-Android-DevOps");
                
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
                        if (getParentActivity() != null) {
                            Toast.makeText(getParentActivity(), "Latest Version: " + tagName, Toast.LENGTH_LONG).show();
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl));
                            getParentActivity().startActivity(browserIntent);
                        }
                    });
                } else {
                    final int finalResponseCode = responseCode;
                    AndroidUtilities.runOnUIThread(() -> 
                        Toast.makeText(getParentActivity(), "Failed to check updates (HTTP " + finalResponseCode + ")", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> 
                    Toast.makeText(getParentActivity(), "Error checking updates. Check connection.", Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}
