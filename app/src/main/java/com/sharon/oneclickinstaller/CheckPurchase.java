package com.sharon.oneclickinstaller;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Inventory;
import com.sharon.oneclickinstaller.util.Purchase;

public class CheckPurchase {

    static IabHelper mHelper;
    static PrefManager prefManager;
    static final String ITEM_SKU_SMALL = "com.sharon.donate_small";
    public static boolean isPremium = false;

    public static void checkpurchases(Context context) {
        prefManager = new PrefManager(context);
        String base64EncodedPublicKey = licensekey();
        mHelper = new IabHelper(context, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    return;
                }
                if (mHelper == null) return;
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    static IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;
            if (result.isFailure()) {
            } else {
                Purchase premiumPurchase = inventory.getPurchase(ITEM_SKU_SMALL);
                if (inventory.hasPurchase(ITEM_SKU_SMALL)) {
                    boolean pre = true;
                }
                boolean premium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
                prefManager.putPremiumInfo("premium",premium);
            }
        }
    };

    static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    private static String licensekey() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiKU4UphDjm82tpzRFVV1chcewlkmomHSZ9U7VGhWAegnEzvxLOk13UhjZzUdxgb9dTI83uYf7ZPl/uPoUuGKX5R10QSFw1NMV8+G2bhlHOrFDNOcGYp2qErW2L9OjBvMVOlWZD8YpQBVj9XhtwHFdMFekUWLTvVPcDd3JVUi8cUGf5xfV828IoN8sB2zFI+FWdLOime1lmRq3JVrKPkEj7+wdS2VAam+g3HYs96kIXVJIw03EgK4mFRibmc0+8xOH7v7TzjKvNMS+fmZnDw7qB27OKrjDV1xrZu2DbrJqIuFtAK8bWRJPZ7/D4h9I1Y/7TQEcM0R0VKEqF5bLNJt3wIDAQAB";
    }

    public static void dispose(){
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}
