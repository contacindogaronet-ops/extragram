package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class DevOpsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private final ArrayList<Item> items = new ArrayList<>();

    // ID Statis untuk routing sub-menu
    private static final int otaUpdateRow = 1;
    private static final int telemetryMetricsRow = 2;
    private static final int backgroundDaemonRow = 3;
    private static final int systemLogsRow = 4;
    private static final int privateTokenRow = 5;

    private static class Item {
        public int id;
        public int viewType;
        public String text;

        private Item(int id, int viewType, String text) {
            this.id = id;
            this.viewType = viewType;
            this.text = text;
        }

        public static Item asHeader(String text) {
            return new Item(0, 1, text);
        }

        public static Item asSetting(int id, String text) {
            return new Item(id, 0, text);
        }

        public static Item asShadow() {
            return new Item(0, 3, null);
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
        items.add(Item.asHeader("DEVOPS & ENGINE PIPELINE"));
        items.add(Item.asSetting(otaUpdateRow, "OTA & Build Updates"));
        items.add(Item.asSetting(telemetryMetricsRow, "Telemetry & Memory Pools"));
        items.add(Item.asSetting(backgroundDaemonRow, "Background Daemon Control"));
        items.add(Item.asSetting(systemLogsRow, "Live System & Kernel Logs"));

        items.add(Item.asHeader("SECURE CONFIG"));
        items.add(Item.asSetting(privateTokenRow, "Manage API & Secret Tokens"));
        items.add(Item.asShadow());
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

        // Routing klik berdasarkan ID statis
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) return;
            Item item = items.get(position);
            
            switch (item.id) {
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
            return holder.getItemViewType() == 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) {
                view = new HeaderCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == 3) {
                view = new ShadowSectionCell(mContext);
            } else {
                view = new TextCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Item item = items.get(position);
            int viewType = holder.getItemViewType();
            if (viewType == 1) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (viewType == 0) {
                ((TextCell) holder.itemView).setText(item.text, true);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }
    }
}
