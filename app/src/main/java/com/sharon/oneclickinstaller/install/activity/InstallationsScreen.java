package com.sharon.oneclickinstaller.install.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;
import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import eu.chainfire.libsuperuser.Shell;

public class InstallationsScreen extends AppCompatActivity {

    public static int NOTIFICATION_ID = 111;
    ArrayList<AppProperties> importedList = new ArrayList<>();
    ArrayList<AppProperties> appList = new ArrayList<>();
    ArrayList<Integer> selectedPositions = new ArrayList<>();
    ArrayList<AppProperties> notInstalledAppsList = new ArrayList<>();
    NotificationManager mNotificationManager;
    Notification.Builder builder;
    TextView notInstalledText, notInstalledTextHeading, progressOperationText;
    int incr = 0;
    Button stopButton;
    int x = 0;
    PrefManager prefManager;
    ArcProgress arcProgress;
    InstallationTask installationTask = new InstallationTask();

    NativeExpressAdView nativeExpressAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_installation);

        prefManager = new PrefManager(this);
        boolean isPremium = prefManager.getPremiumInfo("premium");
        nativeExpressAdView = (NativeExpressAdView) findViewById(R.id.nativeAdViewInstaller);
        if (!isPremium) {
            AdRequest request = new AdRequest.Builder().addTestDevice("A3097AD8C3C34E010D834944ED9D0291").build();
            nativeExpressAdView.loadAd(request);
        }

        setTitle(getString(R.string.install_title));
        arcProgress = (ArcProgress) findViewById(R.id.arc_progress);
        stopButton = (Button) findViewById(R.id.stopButton);
        notInstalledText = (TextView) findViewById(R.id.not_installed_backedup_apps_list);
        notInstalledTextHeading = (TextView) findViewById(R.id.not_installed_backedup_apps_list_heading);
        progressOperationText = (TextView) findViewById(R.id.progress_operation_text);
        selectedPositions = (ArrayList<Integer>) getIntent().getSerializableExtra("selectedpositions");
        importedList = (ArrayList<AppProperties>) getIntent().getSerializableExtra("applist");

        for (int i = selectedPositions.size() - 1; i >= 0; i--) {
            appList.add(importedList.get(selectedPositions.get(i)));
        }
        installationTask.execute();

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(InstallationsScreen.this)
                        .setTitle(getApplicationContext().getString(R.string.backup_stop_button_error))
                        .setMessage(getApplicationContext().getString(R.string.install_stop_button_message))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                installationTask.cancel(true);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    private void startNotification(String contentTitle, String contentText) {

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                //.setAutoCancel(true)
                .setContentTitle(contentTitle)
                //.setContentText(contentText)
                .setPriority(Notification.PRIORITY_HIGH);
//
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, InstallationsScreen.class), PendingIntent.FLAG_UPDATE_CURRENT);
//        builder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void installApkProcess(final AppProperties app) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                x = x + (100 / selectedPositions.size());
                arcProgress.setProgress(x);
                progressOperationText.setText(getApplicationContext().getString(R.string.notification_installing) + incr + "/" + appList.size() + "\t" + app.getAppname());
            }
        });
        String newFilename = "\"" + app.getApkpath() + "\"";
        File file = new File(app.getApkpath());
        if (file.exists()) {
            try {
                if (Shell.SU.available()) {
                    String command = "pm install " + newFilename;
                    Shell.SU.run(command);
                } else {
                    Uri uri;
                    Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        promptInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                    } else {
                        uri = Uri.fromFile(file);
                    }
                    promptInstall.setDataAndType(uri,
                            "application/vnd.android.package-archive");
                    startActivity(promptInstall);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            notInstalledAppsList.add(app);
        }
    }

    private class InstallationTask extends AsyncTask {

        @Override
        protected Void doInBackground(Object[] params) {
            startNotification(getApplicationContext().getString(R.string.app_name), getApplicationContext().getString(R.string.notification_install_default));
            Collections.reverse(appList);
            for (AppProperties temp : appList) {
                while (!isCancelled()) {
                    incr += 1;
                    builder.setProgress(appList.size(), incr, false);
                    builder.setContentTitle(getApplicationContext().getString(R.string.notification_installing));
                    builder.setContentText(incr + "/" + appList.size() + "\t" + temp.getAppname());
                    builder.setOngoing(true);
                    builder.setSmallIcon(R.mipmap.ic_launcher);
                    builder.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher));
                    mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                    try {
                        Thread.sleep(3 * 1000);
                    } catch (InterruptedException e) {
                    }
                    installApkProcess(temp);
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            progressOperationText.setVisibility(View.GONE);
            if (!notInstalledAppsList.isEmpty() || notInstalledAppsList.size() != 0) {
                for (AppProperties appProperties : notInstalledAppsList) {
                    notInstalledText.append(appProperties.getAppname() + "\n");
                }
                notInstalledText.setVisibility(View.VISIBLE);
                notInstalledTextHeading.setVisibility(View.VISIBLE);
                notInstalledTextHeading.setText(getApplicationContext().getString(R.string.not_installed_textview_heading));
            } else {
                builder.setContentText(getApplicationContext().getString(R.string.success_install))
                        .setContentTitle(getApplicationContext().getString(R.string.app_name))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                arcProgress.setProgress(100);
                stopButton.setText(R.string.notification_install_default);
                stopButton.setActivated(false);
                stopButton.setEnabled(false);
                stopButton.setBackground(ContextCompat.getDrawable(InstallationsScreen.this,R.drawable.stop_button_shape_success));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            stopButton.setBackground(ContextCompat.getDrawable(InstallationsScreen.this,R.drawable.stop_button_shape_cancel));
            stopButton.setText(R.string.cancelled);
            stopButton.setActivated(false);
            stopButton.setEnabled(false);
            progressOperationText.setVisibility(View.GONE);
            builder.setContentText(getApplicationContext().getString(R.string.task_cancelled))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    arcProgress.setProgress(0);
                }
            });
        }
    }
}
