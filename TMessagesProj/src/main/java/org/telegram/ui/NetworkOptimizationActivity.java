package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class NetworkOptimizationActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int bufferSizeRow;
    private int tcpNoDelayRow;
    private int aggressiveRow;
    private int rowCount;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;
        bufferSizeRow = rowCount++;
        tcpNoDelayRow = rowCount++;
        aggressiveRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Network & Stream Optimization");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == bufferSizeRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle("Socket Buffer Size");
                CharSequence[] items = new CharSequence[]{
                    "32 KB (Anti-Jitter)",
                    "64 KB (Default Standard)",
                    "256 KB (High Speed)",
                    "1 MB (VVIP Stream)",
                    "4 MB (Zero-Loss VVIP/MTProto)"
                };
                int[] values = {32768, 65536, 262144, 1048576, 4194304};
                builder.setItems(items, (dialog, which) -> {
                    SharedConfig.socketBufferSize = values[which];
                    SharedConfig.saveConfig();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
                showDialog(builder.create());
            } else if (position == tcpNoDelayRow) {
                SharedConfig.tcpNoDelay = !SharedConfig.tcpNoDelay;
                SharedConfig.saveConfig();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.tcpNoDelay);
                }
            } else if (position == aggressiveRow) {
                SharedConfig.aggressiveTearDown = !SharedConfig.aggressiveTearDown;
                SharedConfig.saveConfig();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.aggressiveTearDown);
                }
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
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                view = new TextDetailCell(mContext);
            } else {
                view = new TextCheckCell(mContext);
            }
            view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    TextDetailCell detailCell = (TextDetailCell) holder.itemView;
                    String desc = "Current: " + (SharedConfig.socketBufferSize / 1024) + " KB";
                    detailCell.setTextAndValue("Socket Buffer Size", desc, true);
                    break;
                case 1:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == tcpNoDelayRow) {
                        checkCell.setTextAndCheck("TCP NoDelay (Disable Nagle)", SharedConfig.tcpNoDelay, true);
                    } else if (position == aggressiveRow) {
                        checkCell.setTextAndCheck("Aggressive Tear-Down (Fast Close)", SharedConfig.aggressiveTearDown, false);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == bufferSizeRow) return 0;
            return 1;
        }
    }
}
