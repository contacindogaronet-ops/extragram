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
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class DevOpsDaemonActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    private int daemonServiceRow;
    private int restartDaemonRow;

    private static class ItemInner {
        public int viewType;
        public String text;
        public boolean booleanValue;
        private ItemInner(int type) { this.viewType = type; }
        public static ItemInner asSetting(String text) {
            ItemInner i = new ItemInner(0); i.text = text; return i;
        }
        public static ItemInner asHeader(String text) {
            ItemInner i = new ItemInner(1); i.text = text; return i;
        }
        public static ItemInner asCheck(String text, boolean val) {
            ItemInner i = new ItemInner(2); i.text = text; i.booleanValue = val; return i;
        }
        public static ItemInner asShadow() { return new ItemInner(3); }
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
        items.add(ItemInner.asHeader("DAEMON SERVICE STATUS"));
        daemonServiceRow = row++;
        items.add(ItemInner.asCheck("Enable Background Native Daemon", true));
        
        restartDaemonRow = row++;
        items.add(ItemInner.asSetting("Force Restart Daemon Engine"));
        items.add(ItemInner.asShadow());
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Background Daemon Control");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        listAdapter = new ListAdapter(context);
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == daemonServiceRow && view instanceof TextCheckCell) {
                boolean val = !((TextCheckCell) view).isChecked();
                ((TextCheckCell) view).setChecked(val);
            }
        });

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;
        public ListAdapter(Context context) { mContext = context; }
        @Override public int getItemCount() { return items.size(); }
        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { 
            int type = holder.getItemViewType();
            return type == 0 || type == 2; 
        }
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) view = new HeaderCell(mContext);
            else if (viewType == 2) view = new TextCheckCell(mContext);
            else if (viewType == 3) view = new ShadowSectionCell(mContext);
            else view = new TextSettingCell(mContext);
            return new RecyclerListView.Holder(view);
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (holder.getItemViewType() == 0) ((TextSettingCell) holder.itemView).setText(item.text, true);
            else if (holder.getItemViewType() == 1) ((HeaderCell) holder.itemView).setText(item.text);
            else if (holder.getItemViewType() == 2) ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, item.booleanValue, true);
        }
        @Override public int getItemViewType(int position) { return items.get(position).viewType; }
    }
}
