package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class DevOpsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private final ArrayList<UIItem> items = new ArrayList<>();

    // Definisikan ID Statis untuk setiap sub-menu DevOps (Konsisten ala exteraGram)
    private static final int otaUpdateRow = 1;
    private static final int telemetryMetricsRow = 2;
    private static final int backgroundDaemonRow = 3;
    private static final int systemLogsRow = 4;
    private static final int privateTokenRow = 5;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        items.clear();

        // Header Pipeline DevOps
        items.add(UIItem.asHeader("DEVOPS & ENGINE PIPELINE"));
        
        // Daftarkan item pakai Factory agar sama persis dengan SettingsActivity
        items.add(SettingCell.Factory.of(otaUpdateRow, IconBackgroundColors.BLUE, R.drawable.msg_settings, "OTA & Build Updates", true));
        items.add(SettingCell.Factory.of(telemetryMetricsRow, IconBackgroundColors.GREEN, R.drawable.msg_settings, "Telemetry & Memory Pools", true));
        items.add(SettingCell.Factory.of(backgroundDaemonRow, IconBackgroundColors.ORANGE, R.drawable.msg_settings, "Background Daemon Control", true));
        items.add(SettingCell.Factory.of(systemLogsRow, IconBackgroundColors.VIOLET, R.drawable.msg_settings, "Live System & Kernel Logs", true));

        // Header Secure Config
        items.add(UIItem.asHeader("SECURE CONFIG"));
        items.add(SettingCell.Factory.of(privateTokenRow, IconBackgroundColors.RED, R.drawable.msg_permissions, "Manage API & Secret Tokens", true));

        items.add(UIItem.asShadow(null));
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("DevOps & Telemetry Hub");
        
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);
        
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter);
        
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Routing switch-case berbasis ID Statis (Clean & Safe)
        listView.setOnItemClickListener((view, position) -> {
            UIItem item = items.get(position);
            if (item == null) return;
            int id = item.id;

            switch (id) {
                case otaUpdateRow:
                    presentFragment(new DevOpsOtaActivity());
                    break;
                case telemetryMetricsRow:
                    presentFragment(new DevOpsTelemetryActivity());
                    break;
                case backgroundDaemonRow:
                    presentFragment(new DevOpsDaemonActivity());
                    break;
                case systemLogsRow:
                    presentFragment(new DevOpsLogsActivity());
                    break;
                case privateTokenRow:
                    presentFragment(new DevOpsTokenActivity());
                    break;
            }
        });

        return fragmentView;
    }

    // Menggunakan Adapter bawaan struktur exteraGram jika sudah tersedia globally, 
    // atau gunakan adapter standar SettingsActivity yang sudah ada.
}
