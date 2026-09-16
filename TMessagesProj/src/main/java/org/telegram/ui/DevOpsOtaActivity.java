package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
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

public class DevOpsOtaActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private final ArrayList<ItemInner> items = new ArrayList<>();

    private int autoCheckRow;
    private int checkNowRow;
    private int releaseChannelRow;
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

        items.add(ItemInner.asHeader("OTA & UPDATE CONFIGURATION"));
        autoCheckRow = row++;
        items.add(ItemInner.asCheck("Auto-check for Updates on Startup", true));
        
        checkNowRow = row++;
        items.add(ItemInner.asSetting("Check for Updates Now"));

        items.add(ItemInner.asHeader("RELEASE CHANNEL"));
        releaseChannelRow = row++;
        items.add(ItemInner.asSetting("Channel: Stable / Release"));

        shadowRow = row++;
        items.add(ItemInner.asShadow());
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("OTA & Build Updates");
        
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

        listView.setOnItemClickListener((view, position) -> {
            if (position == checkNowRow) {
                // Trigger logika cek update manual ke GitHub API
                AndroidUtilities.runOnUIThread(() -> {
                    // Tampilkan toast atau dialog pengecekan
                });
            } else if (position == autoCheckRow) {
                if (view instanceof TextCheckCell) {
                    boolean val = !((TextCheckCell) view).isChecked();
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == releaseChannelRow) {
                // Ubah channel build (Stable / Nightly)
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
            return type == 0 || type == 2;
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
                    view = new TextSettingCell(mContext);
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
                    TextSettingCell textCell = (TextSettingCell) holder.itemView;
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
}
