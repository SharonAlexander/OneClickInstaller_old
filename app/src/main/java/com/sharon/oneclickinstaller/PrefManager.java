package com.sharon.oneclickinstaller;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

public class PrefManager {

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    private int PRIVATE_MODE = 0;
    private String IS_FIRST_TIME_LAUNCH = "isFirstTimeLaunch";
    private String PREF_NAME = "one_click_installer";

    public PrefManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME,PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime){
        editor.putBoolean(IS_FIRST_TIME_LAUNCH,isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch(){
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH,true);
    }

    public String getStoragePref(String key) {
        return pref.getString(key, Environment.getExternalStorageDirectory().toString() + "/One_Click_Installer/");
    }

    public void putTreeUri(String key,Uri uri){
        editor.putString(key,uri.toString());
        editor.commit();
    }

    public Uri getTreeUri(String key){
        return Uri.parse(pref.getString(key,Environment.getExternalStorageDirectory().toString()));
    }

    public void putStoragePref(String key, String path) {
        editor.putString(key,path);
        editor.commit();
    }

    public void putLayoutChange(String key,boolean bool){
        editor.putBoolean(key,bool);
        editor.commit();
    }

    public boolean getLayoutChange(String key){
        return pref.getBoolean(key,false);
    }

    public boolean getPremiumInfo(String key) {
        return pref.getBoolean(key,false);
    }

    public void putPremiumInfo(String key,boolean bool){
        editor.putBoolean(key,bool);
        editor.commit();
    }
}
