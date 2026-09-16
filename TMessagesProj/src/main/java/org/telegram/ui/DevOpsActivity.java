package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.ui.ActionBar.Theme;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class DevOpsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private final ArrayList<ItemInner> items = new ArrayList<>();

    // Definisikan row index untuk tiap sub-menu di dalam DevOps
    private int otaUpdateRow;
    private int telemetryMetricsRow;
    private int backgroundDaemonRow;
    private int systemLogsRow;
    private int headerPrivateRow;
    private int privateTokenRow;
    private int shadowRow;

    private static class ItemInner {
        public int viewType;
        public String text;
        public boolean booleanValue;

        private ItemInner(int type) {
            this.viewType = type;
        }

        public static ItemInner asSetting(String text) {
            ItemInner item = new ItemInner(0);
            item.text = text;
            return item;
        }

        public static ItemInner asHeader(String text) {
            ItemInner item = new ItemInner(1);
            item.text = text;
            return item;
        }

        public static ItemInner asCheck(String text, boolean value) {
            ItemInner item = new ItemInner(2);
            item.text = text;
            item.booleanValue = value;
            return item;
        }

        public static ItemInner asShadow() {
            return new ItemInner(3);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        items.clear();
        int row = 0;

        // Pendaftaran menu utama di dalam DevOps Hub
        items.add(ItemInner.asHeader("DEVOPS & ENGINE PIPELINE"));
        otaUpdateRow = row++;
        items.add(ItemInner.asSetting("OTA & Build Updates"));
        
        telemetryMetricsRow = row++;
        items.add(ItemInner.asSetting("Telemetry & Memory Pools"));
        
        backgroundDaemonRow = row++;
        items.add(ItemInner.asSetting("Background Daemon Control"));
        
        systemLogsRow = row++;
        items.add(ItemInner.asSetting("Live System & Kernel Logs"));

        // Bagian privat atau konfigurasi tambahan
        headerPrivateRow = row++;
        items.add(ItemInner.asHeader("SECURE CONFIG & TOKENS"));
        
        privateTokenRow = row++;
        items.add(ItemInner.asSetting("Manage API & Secret Tokens"));

        shadowRow = row++;
        items.add(ItemInner.asShadow());
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

        // Handler klik berdasarkan posisi item yang didaftarkan lewat items.add
        listView.setOnItemClickListener((view, position) -> {
            if (position == otaUpdateRow) {
                   presentFragment(new DevOpsOtaActivity());
            } else if (position == telemetryMetricsRow) {
                   presentFragment(new DevOpsTelemetryActivity());
            } else if (position == backgroundDaemonRow) {
                   presentFragment(new DevOpsDaemonActivity());
            } else if (position == systemLogsRow) {
                   presentFragment(new DevOpsLogsActivity());
            } else if (position == privateTokenRow) {
                   presentFragment(new DevOpsTokenActivity());
            }
        });

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 0 || type == 2; // Hanya cell tipe setting/check yang bisa diklik
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 1:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 0:
                default:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            switch (holder.getItemViewType()) {
                case 0:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setText(item.text, true);
                    break;
                case 1:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(item.text);
                    break;
                case 2:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    checkCell.setTextAndCheck(item.text, item.booleanValue, true);
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }
}
