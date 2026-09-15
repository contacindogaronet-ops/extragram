package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class DevOpsActivity extends BaseFragment {

    private UniversalAdapter adapter;

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

        adapter = new UniversalAdapter(listView, context, currentAccount, 0, this::fillItems, this::onRowClick);
        listView.setAdapter(adapter);

        fragmentView = listView;
        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Database Engine"));
        items.add(UItem.asButton(201, "SQLite Sync Mode", SharedConfig.getSqliteSyncMode()));

        UItem walItem = UItem.asCheck(202, "SQLite WAL Mode");
        walItem.checked = SharedConfig.isSqliteWalEnabled();
        items.add(walItem);

        items.add(UItem.asButton(203, "Database Maintenance", "VACUUM"));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Memory & Engine"));
        items.add(UItem.asButton(204, "Purge RAM & Native GC", "Run"));
        items.add(UItem.asButton(205, "Restart MTProto Daemon", "Reset"));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Build Information"));
        items.add(UItem.asButton(208, "Build Commit Hash", BuildVars.BUILD_GIT_HASH));
    }

    private void onRowClick(UItem item, View view) {
        if (item == null) return;

        switch (item.id) {
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

            case 208:
                AndroidUtilities.addToClipboard(BuildVars.BUILD_GIT_HASH);
                Toast.makeText(getParentActivity(), "Commit Hash Copied: " + BuildVars.BUILD_GIT_HASH, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
