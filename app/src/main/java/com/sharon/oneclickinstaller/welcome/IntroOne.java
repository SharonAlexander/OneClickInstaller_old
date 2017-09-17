package com.sharon.oneclickinstaller.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sharon.oneclickinstaller.CheckPurchase;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;
import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Inventory;
import com.sharon.oneclickinstaller.util.Purchase;

public class IntroOne extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.welcome_slide1, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CheckPurchase.checkpurchases(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CheckPurchase.dispose();
    }
}
