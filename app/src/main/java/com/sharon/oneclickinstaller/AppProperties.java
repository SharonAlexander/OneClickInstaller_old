package com.sharon.oneclickinstaller;

import android.graphics.drawable.Drawable;

import java.io.Serializable;


public class AppProperties implements Serializable{

    private String appname, pname, versionname, apkpath, apksize;
    private int versioncode;
    private boolean already_installed;
    private transient Drawable icon;

    public AppProperties() {
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getVersionname() {
        return versionname;
    }

    public void setVersionname(String versionname) {
        this.versionname = versionname;
    }

    public int getVersioncode() {
        return versioncode;
    }

    public void setVersioncode(int versioncode) {
        this.versioncode = versioncode;
    }

    public String getApkpath() {
        return apkpath;
    }

    public void setApkpath(String apkpath) {
        this.apkpath = apkpath;
    }

    public String getApksize() {
        return apksize;
    }

    public void setApksize(String apksize) {
        this.apksize = apksize;
    }

    public boolean isAlready_installed() {
        return already_installed;
    }

    public void setAlready_installed(boolean already_installed) {
        this.already_installed = already_installed;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

}
