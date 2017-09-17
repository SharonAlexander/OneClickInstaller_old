package com.sharon.oneclickinstaller.install.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;
import com.sharon.oneclickinstaller.Settings;
import com.sharon.oneclickinstaller.install.adapter.InstallerAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class InstallerActivity extends Fragment implements InstallerAdapter.InstallerAdapterListener, EasyPermissions.PermissionCallbacks {

    private static final int WRITE_PERMISSION_CALLBACK_CONSTANT = 111;
    String pathToScan;
    PrefManager preferenceManager;
    boolean layoutChange = false;
    AdView mAdView;
    InterstitialAd mInterstitialAd;
    AdRequest bannerAdRequest;
    boolean isPremium = false;
    private ArrayList<AppProperties> appList;
    private RecyclerView recyclerView;
    private InstallerAdapter mAdapter;
    private WaveSwipeRefreshLayout swipeRefreshLayout;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    private Button change_directory;
    private TextView info_to_refresh;
    private int loadScreen = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        appList = new ArrayList<>();

        MobileAds.initialize(getActivity(), getActivity().getString(R.string.ads_app_id));

        preferenceManager = new PrefManager(getActivity());
        pathToScan = preferenceManager.getStoragePref(getActivity().getString(R.string.preference_path_to_scan));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_installer, container, false);

        isPremium = preferenceManager.getPremiumInfo("premium");
        mAdView = (AdView) rootView.findViewById(R.id.adListViewbanner);
        if (!isPremium) {
            adsInitialise();
            requestNewInterstitial();
        } else {
            mAdView.setVisibility(View.GONE);
        }

        getActivity().setTitle(getActivity().getString(R.string.installer_title));
        recyclerView = (RecyclerView) rootView.findViewById(R.id.installer_recycler_view);
        swipeRefreshLayout = (WaveSwipeRefreshLayout) rootView.findViewById(R.id.installer_backup_swipe_refresh_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Integer> selectedItemPositions =
                        (ArrayList<Integer>) mAdapter.getSelectedItems();
                if (selectedItemPositions.isEmpty()) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.no_selection), Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getActivity(), InstallationsScreen.class);
                    intent.putExtra("applist", appList);
                    intent.putExtra("selectedpositions", selectedItemPositions);
                    startActivity(intent);
                    actionMode.finish();
                }
            }
        });
        info_to_refresh = (TextView) rootView.findViewById(R.id.info_to_refresh);
        change_directory = (Button) rootView.findViewById(R.id.change_directory_button);
        change_directory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPremium) {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    } else {
                        Intent settings = new Intent(getActivity(), Settings.class);
                        startActivity(settings);
                    }
                } else {
                    Intent settings = new Intent(getActivity(), Settings.class);
                    startActivity(settings);
                }
            }
        });
        actionModeCallback = new ActionModeCallback();
        getReadPermission();
        swipeRefreshLayout.setWaveColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getReadPermission();
            }
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            if (!isPremium) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Intent settings = new Intent(getActivity(), Settings.class);
                    startActivity(settings);
                }
            } else {
                Intent settings = new Intent(getActivity(), Settings.class);
                startActivity(settings);
            }
        }
        if (id == R.id.layout) {
            if (!layoutChange) {
                recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            }
            layoutChange = !layoutChange;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<AppProperties> getAllApks(String pathToScan) {
        File file = new File(pathToScan);
        File fileLists[] = file.listFiles();
        if (fileLists != null && fileLists.length > 0) {
            for (File filename : fileLists) {
                if (filename.isDirectory()) {
                    getAllApks(filename.getPath());
                } else {
                    if (filename.getName().endsWith(".apk")) {
                        PackageManager packageManager = getActivity().getPackageManager();
                        PackageInfo pi = packageManager.getPackageArchiveInfo(filename.getAbsolutePath(), 0);
                        if (pi == null)
                            continue;
                        pi.applicationInfo.sourceDir = filename.getAbsolutePath();
                        pi.applicationInfo.publicSourceDir = filename.getAbsolutePath();

                        AppProperties app = new AppProperties();
                        app.setAppname((String) pi.applicationInfo.loadLabel(packageManager));
                        app.setPname(pi.packageName);
                        app.setApkpath(filename.getAbsolutePath());
                        app.setIcon(pi.applicationInfo.loadIcon(packageManager));
                        app.setApksize((filename.length() / (1024 * 1024)) + "MB");
                        app.setVersionname(pi.versionName);
                        if (null != packageManager.getLaunchIntentForPackage(pi.applicationInfo.packageName)) {
                            app.setAlready_installed(true);
                        } else {
                            app.setAlready_installed(false);
                        }
                        appList.add(app);
                    }
                }
            }
        }
        return appList;
    }

    @Override
    public void onIconClicked(int position) {

    }

    @Override
    public void onIconImportantClicked(int position) {

    }

    @Override
    public void onMessageRowClicked(int position) {
        if (mAdapter.getSelectedItemCount() > 0) {
            enableActionMode(position);
        } else {
            AppProperties app = appList.get(position);
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(app.getIcon());
            String message = "<b>Version</b><br>" + app.getVersionname() + "<br><br><b>Package Name</b><br>" + app.getPname() + "<br><br><b>Size</b><br>" + app.getApksize() + "<br><br><b>Path</b><br>" + app.getApkpath();
            builder.setTitle(app.getAppname())
                    .setMessage(Html.fromHtml(message))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //
                        }
                    });
            builder.create();
            builder.show();
        }
    }

    @Override
    public void onRowLongClicked(int position) {
        enableActionMode(position);
    }

    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    @AfterPermissionGranted(WRITE_PERMISSION_CALLBACK_CONSTANT)
    private void deleteFiles() {
        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            List<Integer> selectedItemPositions =
                    mAdapter.getSelectedItems();
            new DeleteApkFiles(selectedItemPositions).execute();
        } else {
            EasyPermissions.requestPermissions(this, getActivity().getString(R.string.rationale_storage),
                    WRITE_PERMISSION_CALLBACK_CONSTANT, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    @AfterPermissionGranted(WRITE_PERMISSION_CALLBACK_CONSTANT)
    private void getReadPermission() {
        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            proceedAfterPermission();
        } else {
            EasyPermissions.requestPermissions(this, getActivity().getString(R.string.rationale_storage),
                    WRITE_PERMISSION_CALLBACK_CONSTANT, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void proceedAfterPermission() {
        new LoadApkFiles().execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isPremium) {
            mAdView.loadAd(bannerAdRequest);
        }
    }

    private void adsInitialise() {
        mAdView.setVisibility(View.VISIBLE);
        bannerAdRequest = new AdRequest.Builder()
                .addTestDevice("A3097AD8C3C34E010D834944ED9D0291")
                .build();

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getActivity().getString(R.string.ads_interstitial_activity_settings_video));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                Intent settings = new Intent(getActivity(), Settings.class);
                startActivity(settings);
            }
        });
    }

    private void requestNewInterstitial() {
        AdRequest interstitialAdRequest = new AdRequest.Builder()
                .addTestDevice("A3097AD8C3C34E010D834944ED9D0291")
                .build();
        mInterstitialAd.loadAd(interstitialAdRequest);
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);
            // disable swipe refresh if action mode is enabled
            swipeRefreshLayout.setEnabled(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteFiles();
                    mode.finish();
                    return true;
                case R.id.action_select_all:
                    int count = 0;
                    for (int i = 0; i < appList.size(); i++) {
                        mAdapter.selectAll(i);
                        count++;
                    }
                    actionMode.setTitle(String.valueOf(count));
                    actionMode.invalidate();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            swipeRefreshLayout.setEnabled(true);
            actionMode = null;
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class LoadApkFiles extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;

        @Override
        protected Void doInBackground(Void... params) {
            pathToScan = preferenceManager.getStoragePref(getActivity().getString(R.string.preference_path_to_scan));
            preferenceManager.putStoragePref(getActivity().getString(R.string.preference_path_to_scan), pathToScan);
            appList = getAllApks(pathToScan);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            appList.clear();
            progressDialog = new ProgressDialog(getActivity());
            if (loadScreen == 0) {
                progressDialog.setMessage(getActivity().getString(R.string.app_loader_process_dialog_message));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            Collections.sort(appList, new Comparator<AppProperties>() {
                public int compare(AppProperties v1, AppProperties v2) {
                    return v1.getAppname().toLowerCase().compareTo(v2.getAppname().toLowerCase());
                }
            });
            mAdapter = new InstallerAdapter(getActivity(), appList, InstallerActivity.this);
            recyclerView.setAdapter(mAdapter);
            if (progressDialog.isShowing() && loadScreen == 0) {
                progressDialog.dismiss();
                loadScreen = 1;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (appList.isEmpty()) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.no_apps_found), Toast.LENGTH_LONG).show();
                change_directory.setVisibility(View.VISIBLE);
                info_to_refresh.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), getActivity().getString(R.string.swipe_down_to_refresh), Toast.LENGTH_SHORT).show();
                change_directory.setVisibility(View.GONE);
                info_to_refresh.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.dismiss();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private class DeleteApkFiles extends AsyncTask<Void, Void, Void> {

        public int NOTIFICATION_ID = 333;
        List<Integer> selectedItemPositions;
        ArrayList<String> notDeleted = new ArrayList<>();
        NotificationManager mNotificationManager;
        Notification.Builder builder;
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        int incr = 0;

        public DeleteApkFiles(List<Integer> selectedItemPositions) {
            this.selectedItemPositions = selectedItemPositions;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            startNotification(getActivity().getString(R.string.app_name), getActivity().getString(R.string.notification_delete_default));
            try {
                doDeletion(selectedItemPositions);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            selectedItemPositions.clear();
            mAdapter.notifyDataSetChanged();
            super.onPostExecute(aVoid);
            if (!notDeleted.isEmpty() || notDeleted.size() != 0) {
                builder.setOngoing(false).setProgress(0, 0, false)
                        .setContentTitle(getActivity().getString(R.string.error_delete_failed))
                        .setContentText(notDeleted.toString().substring(1, notDeleted.toString().length() - 1));
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            } else {
                builder.setContentText(getActivity().getString(R.string.success_delete))
                        // Removes the progress bar
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        }

        private void deleteApk(AppProperties temp) {
            boolean success = false;
            File file = new File(temp.getApkpath());
            if (temp.getApkpath().contains(Environment.getExternalStorageDirectory().toString())) {
                success = file.delete();
            } else {
                success = deleteUriApk(temp);
            }
            if (!success) {
                if (Shell.SU.available()) {
                    String command = "rm " + "\"" + file + "\"";
                    Shell.SU.run(command);
                    success = true;
                }
            }
            if (success) {
            } else {
                notDeleted.add(temp.getAppname());
            }
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        private boolean deleteUriApk(AppProperties temp) {
            boolean success = false;
            String folder = preferenceManager.getStoragePref(getString(R.string.preference_path_to_scan));
            folder = folder.substring(folder.lastIndexOf("/") + 1);
            String filename = temp.getApkpath().substring(temp.getApkpath().lastIndexOf("/") + 1);
            DocumentFile pickedDir = DocumentFile.fromTreeUri(getActivity(), preferenceManager.getTreeUri("treeuri"));
            try {
                pickedDir = pickedDir.findFile(folder);
                pickedDir = pickedDir.findFile(filename);
                success = DocumentsContract.deleteDocument(getActivity().getContentResolver(), pickedDir.getUri());
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return success;
        }

        private void doDeletion(List<Integer> selectedItemPositions) throws Exception {
            Collections.sort(selectedItemPositions);
            Collections.reverse(selectedItemPositions);
            for (int i : selectedItemPositions) {
                deleteApk(appList.get(i));
                mAdapter.removeData(i);
            }
        }

        private void startNotification(String contentTitle, String contentText) {

            mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            //Build the notification using Notification.Builder
            builder = new Notification.Builder(getActivity())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setPriority(Notification.PRIORITY_HIGH);


            //Show the notification
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}