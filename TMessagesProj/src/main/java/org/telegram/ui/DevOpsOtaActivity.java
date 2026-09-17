package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DevOpsOtaActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private final ArrayList<Item> items = new ArrayList<>();

    // State Manajemen OTA
    private boolean isChecking = false;
    private boolean isDownloading = false;
    private boolean isDownloadedReady = false;
    private int downloadProgress = 0;
    
    private String versionTitle = "Pemeriksaan Sistem";
    private String versionStatus = "Tekan tombol di bawah untuk memeriksa pembaruan dari GitHub.";
    private String releaseBody = "Belum ada catatan rilis yang dimuat.";
    private String directApkDownloadUrl = "";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_STATUS_CARD = 1;
    private static final int TYPE_CHANGELOG = 2;
    private static final int TYPE_ACTION_BUTTON = 3;

    private static class Item {
        public int viewType;
        public String title;
        public String subtitle;

        public Item(int viewType, String title, String subtitle) {
            this.viewType = viewType;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        rebuildUIModel();
        checkForGithubRelease(false);
        return true;
    }

    private void rebuildUIModel() {
        items.clear();
        items.add(new Item(TYPE_HEADER, "GITHUB RELEASE PIPELINE", null));
        items.add(new Item(TYPE_STATUS_CARD, versionTitle, versionStatus));
        items.add(new Item(TYPE_CHANGELOG, "• Catatan Rilis / Changelog:\n" + releaseBody, "GitHub Latest Tag"));

        String actionTitle;
        if (isDownloadedReady) {
            actionTitle = "Pasang Pembaruan Sekarang (Install)";
        } else if (isDownloading) {
            actionTitle = "Mengunduh APK... (" + downloadProgress + "%)";
        } else if (isChecking) {
            actionTitle = "Memeriksa GitHub API...";
        } else {
            actionTitle = "Periksa Pembaruan / Unduh APK";
        }
        items.add(new Item(TYPE_ACTION_BUTTON, actionTitle, null));
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("GitHub OTA Manager");

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

        listView.setOnItemClickListener((view, position) -> {
            if (items.get(position).viewType == TYPE_ACTION_BUTTON) {
                Context ctx = getParentActivity();
                if (ctx == null) ctx = context;
                
                if (isDownloadedReady) {
                    triggerApkInstallation(ctx);
                } else if (!isDownloading && !isChecking) {
                    if (directApkDownloadUrl.isEmpty()) {
                        checkForGithubRelease(true);
                    } else {
                        startOtaDownloadPipeline(ctx);
                    }
                }
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void checkForGithubRelease(boolean showToast) {
        isChecking = true;
        refreshUI();

        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/contacindogaronet-ops/extragram/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "ExtraGram-OTA-Client");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    String tagName = json.optString("tag_name", "v13.x");
                    releaseBody = json.optString("body", "Tidak ada deskripsi rilis.");
                    
                    JSONArray assets = json.optJSONArray("assets");
                    if (assets != null && assets.length() > 0) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            String name = asset.optString("name", "");
                            if (name.endsWith(".apk")) {
                                directApkDownloadUrl = asset.optString("browser_download_url", "");
                                break;
                            }
                        }
                    }

                    AndroidUtilities.runOnUIThread(() -> {
                        isChecking = false;
                        versionTitle = "Rilis Terbaru: " + tagName;
                        versionStatus = directApkDownloadUrl.isEmpty() ? "File APK belum dilampirkan di asset rilis." : "APK siap diunduh dari repository.";
                        refreshUI();
                        if (showToast && getParentActivity() != null) {
                            Toast.makeText(getParentActivity(), "Berhasil memuat rilis " + tagName, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    throw new Exception("HTTP Error Code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    isChecking = false;
                    versionTitle = "Gagal Memeriksa Pembaruan";
                    versionStatus = "Periksa koneksi internet Anda.";
                    refreshUI();
                    if (showToast && getParentActivity() != null) {
                        Toast.makeText(getParentActivity(), "Gagal terhubung ke GitHub API.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void startOtaDownloadPipeline(Context context) {
        if (directApkDownloadUrl.isEmpty()) {
            Toast.makeText(context, "URL Download APK tidak ditemukan!", Toast.LENGTH_SHORT).show();
            return;
        }

        isDownloading = true;
        downloadProgress = 0;
        versionStatus = "Mengunduh app.apk dari GitHub Releases...";
        refreshUI();

        new Thread(() -> {
            try {
                URL url = new URL(directApkDownloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();
                if (fileLength <= 0) fileLength = 1024 * 1024 * 75;

                File downloadedApkFile = new File(context.getExternalFilesDir(null), "extragram_update.apk");
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(downloadedApkFile);

                byte[] buffer = new byte[8192];
                long totalBytesRead = 0;
                int count;

                while ((count = inputStream.read(buffer)) != -1) {
                    totalBytesRead += count;
                    downloadProgress = (int) ((totalBytesRead * 100) / fileLength);
                    if (downloadProgress > 100) downloadProgress = 100;

                    AndroidUtilities.runOnUIThread(this::refreshUI);
                    outputStream.write(buffer, 0, count);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                AndroidUtilities.runOnUIThread(() -> {
                    isDownloading = false;
                    isDownloadedReady = true;
                    versionStatus = "Download selesai. Siap dipasang.";
                    refreshUI();
                    Toast.makeText(context, "APK OTA Berhasil Diunduh!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    isDownloading = false;
                    downloadProgress = 0;
                    versionStatus = "Download gagal: " + e.getLocalizedMessage();
                    refreshUI();
                    Toast.makeText(context, "Gagal mengunduh file APK.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void triggerApkInstallation(Context context) {
        File downloadedApkFile = new File(context.getExternalFilesDir(null), "extragram_update.apk");
        if (!downloadedApkFile.exists()) {
            Toast.makeText(context, "File APK tidak ditemukan!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", downloadedApkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(downloadedApkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(context, "Gagal membuka installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshUI() {
        rebuildUIModel();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
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
            return holder.getItemViewType() == TYPE_ACTION_BUTTON;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) {
                TextView tv = new TextView(mContext);
                tv.setTextSize(13);
                tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
                view = tv;
            } else if (viewType == TYPE_STATUS_CARD) {
                LinearLayout layout = new LinearLayout(mContext);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
                layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                
                TextView titleTv = new TextView(mContext);
                titleTv.setTextSize(16);
                titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                titleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                titleTv.setTag("title");
                layout.addView(titleTv);

                TextView subTv = new TextView(mContext);
                subTv.setTextSize(14);
                subTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                subTv.setPadding(0, AndroidUtilities.dp(4), 0, 0);
                subTv.setTag("subtitle");
                layout.addView(subTv);

                view = layout;
            } else if (viewType == TYPE_CHANGELOG) {
                LinearLayout layout = new LinearLayout(mContext);
                layout.setOrientation(LinearLayout.VERTICAL);
                
                // Tambahkan margin top supaya ada jarak dan tidak menumpuk dengan card di atasnya
                RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, AndroidUtilities.dp(8), 0, 0);
                layout.setLayoutParams(params);

                layout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
                layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

                TextView subTv = new TextView(mContext);
                subTv.setTextSize(13);
                subTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                subTv.setTag("changelog_text");
                layout.addView(subTv);

                view = layout;
            } else {
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                btnParams.setMargins(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

                TextView btn = new TextView(mContext);
                btn.setLayoutParams(btnParams);
                btn.setGravity(android.view.Gravity.CENTER);
                btn.setTextSize(15);
                btn.setTypeface(null, android.graphics.Typeface.BOLD);
                btn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
                btn.setBackgroundColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                btn.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(14));
                view = btn;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Item item = items.get(position);
            int type = holder.getItemViewType();

            if (type == TYPE_HEADER) {
                ((TextView) holder.itemView).setText(item.title);
            } else if (type == TYPE_STATUS_CARD) {
                ViewGroup group = (ViewGroup) holder.itemView;
                ((TextView) group.findViewWithTag("title")).setText(item.title);
                ((TextView) group.findViewWithTag("subtitle")).setText(item.subtitle);
            } else if (type == TYPE_CHANGELOG) {
                ViewGroup group = (ViewGroup) holder.itemView;
                ((TextView) group.findViewWithTag("changelog_text")).setText(item.title);
            } else if (type == TYPE_ACTION_BUTTON) {
                ((TextView) holder.itemView).setText(item.title);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }
    }
}
