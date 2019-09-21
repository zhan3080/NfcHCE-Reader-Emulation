package com.example.nfcapplication;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;

public class NfcReadActivity extends Activity {
    private static final String TAG = "NfcReadActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_read);
        initData();
    }

    private void initData(){
        NfcUtils nfcUtils = new NfcUtils(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcUtils.mNfcAdapter.enableForegroundDispatch(this, NfcUtils.mPendingIntent, NfcUtils.mIntentFilter, NfcUtils.mTechList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NfcUtils.mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String[] techList = detectedTag.getTechList();
        Ndef ndef = Ndef.get(detectedTag);
        Log.i(TAG, "onNewIntent 标签支持的tachnology类型：");
        for (String tech : techList) {
            Log.i(TAG, "onNewIntent:" + tech);
        }
        try {
            Log.i(TAG, "onNewIntent nfcId:" + NfcUtils.readNFCId(intent));
            String str = NfcUtils.readNFCFromTag(intent);
            Log.i(TAG,"onNewIntent str:"+str);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
