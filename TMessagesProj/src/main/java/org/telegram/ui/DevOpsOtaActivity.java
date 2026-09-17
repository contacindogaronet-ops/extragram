package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DevOpsOtaActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private final ArrayList<Item> items = new ArrayList<>();

    // State Download & OTA
    private boolean isDownloading = false;
    private int downloadProgress = 0;
    private String statusText = "Stable v3.7.2 - Up to date";

    // Row IDs
    private static final int rowHeaderInfo = 1;
    private static final int rowStatusDetail = 2;
    private static final int rowActionCheck = 3;
    private static final int rowActionDownload = 4;
    private static final int rowShadow = 5;

    private static class Item {
        public int id;
        public int viewType;
        public String title;
        public String subtitle;

        private Item(int id, int viewType, String title, String subtitle) {
            this.id = id;
            this.viewType = viewType;
            this.title = title;
            this.subtitle = subtitle;
        }

        public static Item asHeader(String title) {
            return new Item(0, 1, title, null);
        }

        public static Item asDetail(int id, String title, String subtitle) {
            return new Item(id, 2, title, subtitle);
        }

        public static Item asAction(int id, String title) {
            return new Item(id, 0, title, null);
        }

        public static Item asShadow() {
            return new Item(0, 3, null, null);
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
        items.add(Item.asHeader("FIRMWARE & OTA PIPELINE"));
        items.add(Item.asDetail(rowStatusDetail, "Current Engine Status", statusText));
        items.add(Item.asAction(rowActionCheck, "Check for Updates via API"));
        
        // Dynamic label based on download state
        String downloadLabel = isDownloading ? "Downloading Update (" + downloadProgress + "%)..." : "Download & Apply Latest Build";
        items.add(Item.asAction(rowActionDownload, downloadLabel));
        
        items.add(Item.asShadow());
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("OTA Build & Update Hub");

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
            if (position < 0 || position >= items.size()) return;
            Item item = items.get(position);

            if (item.id == rowActionCheck) {
                checkForUpdatesRemote();
            } else if (item.id == rowActionDownload) {
                if (!isDownloading) {
                    startOtaDownloadPipeline(context);
                } else {
                    Toast.makeText(context, "Download is already in progress...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return fragmentView;
    }

    private void checkForUpdatesRemote() {
        statusText = "Checking repository mirrors...";
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }

        // Simulasi network check yang bersih & responsif
        AndroidUtilities.runOnUIThread(() -> {
            statusText = "New build available: v3.8.0-beta";
            updateRows();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            Toast.makeText(getParentActivity(), "Update found!", Toast.LENGTH_SHORT).show();
        }, 1200);
    }

    private void startOtaDownloadPipeline(Context context) {
        isDownloading = true;
        downloadProgress = 0;
        statusText = "Downloading package...";
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }

        // Background worker thread untuk stream download tanpa blocking UI thread
        new Thread(() -> {
            try {
                // Contoh endpoint APK/Payload OTA modular
                String fileUrl = "https://raw.githubusercontent.com/contacindogaronet-ops/exteragram/main/payload.bin";
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();
                if (fileLength <= 0) fileLength = 1024 * 1024 * 15; // Fallback estimate 15MB

                File outputFile = new File(context.getExternalFilesDir(null), "exteragram_update.apk");
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                byte[] buffer = new byte[8192];
                long totalBytesRead = 0;
                int count;

                while ((count = inputStream.read(buffer)) != -1) {
                    totalBytesRead += count;
                    downloadProgress = (int) ((totalBytesRead * 100) / fileLength);
                    if (downloadProgress > 100) downloadProgress = 100;

                    // Update UI secara berkala lewat main thread
                    AndroidUtilities.runOnUIThread(() -> {
                        updateRows();
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    });

                    outputStream.write(buffer, 0, count);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                AndroidUtilities.runOnUIThread(() -> {
                    isDownloading = false;
                    statusText = "Download complete. Ready to install.";
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(context, "OTA Package downloaded successfully!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    isDownloading = false;
                    downloadProgress = 0;
                    statusText = "Download failed: Check network configuration";
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(context, "Download Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
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
            return type == 0; // Hanya item action yang bisa diklik
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) {
                view = new HeaderCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == 2) {
                view = new TextDetailCell(mContext);
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
                ((HeaderCell) holder.itemView).setText(item.title);
            } else if (viewType == 2) {
                ((TextDetailCell) holder.itemView).setTextAndValue(item.title, item.subtitle, true);
            } else if (viewType == 0) {
                ((TextCell) holder.itemView).setText(item.title, true);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }
    }
}
