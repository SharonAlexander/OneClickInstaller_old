package com.sharon.oneclickinstaller.backup.activity;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.sharon.oneclickinstaller.backup.adapter.BackupAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class BackupActivity extends Fragment implements BackupAdapter.BackupAdapterListener, EasyPermissions.PermissionCallbacks {

    private static final int WRITE_PERMISSION_CALLBACK_CONSTANT = 333;
    PrefManager preferenceManager;
    boolean layoutChange = false;
    AdView mAdView;
    InterstitialAd mInterstitialAd;
    AdRequest bannerAdRequest;
    boolean isPremium = false;
    int tryx = 0;
    private ArrayList<ApplicationInfo> appList;
    private ArrayList<AppProperties> backedupAppList;
    private RecyclerView recyclerView;
    private BackupAdapter mAdapter;
    private WaveSwipeRefreshLayout swipeRefreshLayout;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    private TextView info_to_refresh;
    private int loadScreen = 0;
    private PackageManager packageManager = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        appList = new ArrayList<>();
        backedupAppList = new ArrayList<>();
        packageManager = getActivity().getPackageManager();
        preferenceManager = new PrefManager(getActivity());

        MobileAds.initialize(getActivity(), getActivity().getString(R.string.ads_app_id));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_backup, container, false);

        isPremium = preferenceManager.getPremiumInfo("premium");
        mAdView = (AdView) rootView.findViewById(R.id.adListViewbanner);
        if (!isPremium) {
            adsInitialise();
            requestNewInterstitial();
        } else {
            mAdView.setVisibility(View.GONE);
        }

        getActivity().setTitle(getActivity().getString(R.string.backup_title));
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
                    Intent intent = new Intent(getActivity(), BackupScreen.class);
                    intent.putExtra("applist", appList);
                    intent.putExtra("selectedpositions", selectedItemPositions);
                    startActivity(intent);
                    actionMode.finish();
                }
            }
        });

        info_to_refresh = (TextView) rootView.findViewById(R.id.info_to_refresh);

        actionModeCallback = new ActionModeCallback();

        getReadPermission();
        swipeRefreshLayout.setWaveColor(ContextCompat.getColor(getActivity(),R.color.colorAccent));
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
            showAppInfoDialog(position);
        }
    }

    private void showAppInfoDialog(int position) {
        PackageInfo pInfo = null;
        ApplicationInfo app = appList.get(position);
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(app.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(app.loadIcon(getActivity().getPackageManager()));
        String message = "<b>Version</b><br>" + pInfo.versionName + "<br><br><b>Package Name</b><br>" + app.packageName + "<br><br><b>Size</b><br>" + (new File(app.publicSourceDir).length() / (1024 * 1024)) + "MB" + "<br><br><b>Path</b><br>" + app.publicSourceDir;
        builder.setTitle(app.loadLabel(getActivity().getPackageManager()))
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //
                    }
                });
        builder.create();
        builder.show();
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

    private ArrayList<ApplicationInfo> checkForLaunchIntent(
            List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> applist = new ArrayList<>();
        for (ApplicationInfo info : list) {
            try {
                if (null != packageManager
                        .getLaunchIntentForPackage(info.packageName)) {
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        applist.add(info);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return applist;
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
                case R.id.action_select_all:
                    tryx++;
                    int count = 0;
                    if (tryx == 1) {
                        for (int i = 0; i < appList.size(); i++) {
                            mAdapter.removeAll(i);
                            ApplicationInfo app = appList.get(i);
                            PackageInfo pi = packageManager.getPackageArchiveInfo(app.publicSourceDir, 0);
                            for (AppProperties appProperties : backedupAppList) {
                                if (appProperties.getPname().equals(pi.packageName) && appProperties.getVersioncode() < pi.versionCode) {
                                    mAdapter.selectUpdated(i);
                                    count++;
                                }
                            }
                        }
                        if (count == 0) {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.backup_no_update_toast_message), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), count + getActivity().getString(R.string.backup_available_update_toast_message), Toast.LENGTH_LONG).show();
                        }
                    } else if (tryx == 2) {
                        for (int i = 0; i < appList.size(); i++) {
                            mAdapter.selectAll(i);
                            count++;
                        }
                        tryx = 0;
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
            tryx = 0;
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
            appList = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
            backedupAppList = getBackedUpApks(preferenceManager.getStoragePref(getActivity().getString(R.string.preference_path_to_scan)));
            mAdapter = new BackupAdapter(getActivity(), appList, backedupAppList, BackupActivity.this);
            Collections.sort(appList,
                    new ApplicationInfo.DisplayNameComparator(packageManager));

            return null;
        }

        private ArrayList<AppProperties> getBackedUpApks(String pathToScan) {
            File file = new File(pathToScan);
            File fileLists[] = file.listFiles();
            if (fileLists != null && fileLists.length > 0) {
                for (File filename : fileLists) {
                    if (filename.isDirectory()) {
                        getBackedUpApks(filename.getPath());
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
                            app.setVersioncode(pi.versionCode);
                            backedupAppList.add(app);
                        }
                    }
                }
            }
            return backedupAppList;

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
            recyclerView.setAdapter(mAdapter);
            if (progressDialog.isShowing() && loadScreen == 0) {
                progressDialog.dismiss();
                loadScreen = 1;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (appList.isEmpty()) {
                info_to_refresh.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), getActivity().getString(R.string.swipe_down_to_refresh), Toast.LENGTH_SHORT).show();
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
}