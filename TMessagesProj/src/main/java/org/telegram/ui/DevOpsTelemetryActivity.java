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
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class DevOpsTelemetryActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    private int poolStatsRow;
    private int zeroCopyStatusRow;

    private static class ItemInner {
        public int viewType;
        public String text;
        public String value;

        private ItemInner(int type) {
            this.viewType = type;
        }

        public static ItemInner asSetting(String text, String val) {
            ItemInner item = new ItemInner(0);
            item.text = text;
            item.value = val;
            return item;
        }

        public static ItemInner asHeader(String text) {
            ItemInner item = new ItemInner(1);
            item.text = text;
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

        items.add(ItemInner.asHeader("MEMORY POOL ALLOCATION"));
        poolStatsRow = row++;
        items.add(ItemInner.asSetting("Dual-Pool (32KB / 4MB)", "Active"));

        items.add(ItemInner.asHeader("KERNEL ROUTING STATUS"));
        zeroCopyStatusRow = row++;
        items.add(ItemInner.asSetting("Linux Splice Zero-Copy", "Optimized"));

        items.add(ItemInner.asShadow());
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Telemetry & Memory Pools");
        
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
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

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;
        public ListAdapter(Context context) { mContext = context; }

        @Override
        public int getItemCount() { return items.size(); }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return false; }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) view = new HeaderCell(mContext);
            else if (viewType == 3) view = new ShadowSectionCell(mContext);
            else view = new TextDetailCell(mContext);
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (holder.getItemViewType() == 0) {
                TextDetailCell cell = (TextDetailCell) holder.itemView;
                cell.setTextAndValue(item.text, item.value, true);
            } else if (holder.getItemViewType() == 1) {
                ((HeaderCell) holder.itemView).setText(item.text);
            }
        }

        @Override
        public int getItemViewType(int position) { return items.get(position).viewType; }
    }
}

