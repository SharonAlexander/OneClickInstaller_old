package com.sharon.oneclickinstaller;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Purchase;

public class Donate extends AppCompatActivity {

    static final String ITEM_SKU_SMALL = "com.sharon.donate_small";
    static final String DONATE_SMALL_THANKS = "1";
    IabHelper mHelper;
    int measureWidth, measureHeight;
    Button donateSmall;
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase) {
            if (result.isFailure()) {
                // Handle error
                new AlertDialog.Builder(Donate.this)
                        .setTitle(R.string.purchase_error)
                        .setMessage(R.string.purchase_already_owned)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(R.mipmap.ic_launcher)
                        .show();
            } else if (purchase.getSku().equals(ITEM_SKU_SMALL)) {
                Message(DONATE_SMALL_THANKS);

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.donation_main);

        setTitle(Donate.class.getSimpleName());

        donateSmall = (Button) findViewById(R.id.donate_small);

        measureWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        measureHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        donateSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mHelper.launchPurchaseFlow(Donate.this, ITEM_SKU_SMALL, 10001,
                            mPurchaseFinishedListener, "donateSmallPurchase");
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        String base64EncodedPublicKey = apilicense();


        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                } else {
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (!mHelper.handleActivityResult(requestCode,
                resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void Message(String message) {

        final Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                builder.dismiss();
            }
        });

        ImageView imageView = new ImageView(this);
        if (message.contentEquals(DONATE_SMALL_THANKS)) {
            imageView.setImageResource(R.drawable.donation_thankyou_large);
        } else {
            imageView.setImageResource(R.drawable.errormsg);
        }
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                measureWidth,
                measureHeight));
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null) try {
            mHelper.dispose();
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
        mHelper = null;
    }

    private String apilicense() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiKU4UphDjm82tpzRFVV1chcewlkmomHSZ9U7VGhWAegnEzvxLOk13UhjZzUdxgb9dTI83uYf7ZPl/uPoUuGKX5R10QSFw1NMV8+G2bhlHOrFDNOcGYp2qErW2L9OjBvMVOlWZD8YpQBVj9XhtwHFdMFekUWLTvVPcDd3JVUi8cUGf5xfV828IoN8sB2zFI+FWdLOime1lmRq3JVrKPkEj7+wdS2VAam+g3HYs96kIXVJIw03EgK4mFRibmc0+8xOH7v7TzjKvNMS+fmZnDw7qB27OKrjDV1xrZu2DbrJqIuFtAK8bWRJPZ7/D4h9I1Y/7TQEcM0R0VKEqF5bLNJt3wIDAQAB";
    }
}

