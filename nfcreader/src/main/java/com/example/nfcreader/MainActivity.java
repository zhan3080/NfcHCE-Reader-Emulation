package com.example.nfcreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends Activity {
    private final String TAG = "MainActivity";
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilter;
    private String[][] mTechList;
    private TextView mTvView;
    // 卡片返回来的正确信号
    private final byte[] SELECT_OK = stringToBytes("9000");
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvView = findViewById(R.id.msgid);
        nfcCheck();
        init();
        Log.i(TAG, "onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilter, mTechList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void init() {
        Log.i(TAG, "init");
        //NFCActivity 一般设置为: SingleTop模式 ，并且锁死竖屏，以避免屏幕旋转Intent丢失
        Intent intent = new Intent(MainActivity.this, MainActivity.class);

        // 私有的请求码
        final int REQUEST_CODE = 1 << 16;

        final int FLAG = 0;
        mPendingIntent = PendingIntent.getActivity(MainActivity.this, REQUEST_CODE, intent, FLAG);

        // 三种过滤器
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mIntentFilter = new IntentFilter[]{ndef, tech, tag};

        // 只针对ACTION_TECH_DISCOVERED
        mTechList = new String[][]{
                {IsoDep.class.getName()}, {NfcA.class.getName()}, {NfcB.class.getName()},
                {NfcV.class.getName()}, {NfcF.class.getName()}, {Ndef.class.getName()}};
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent ");
        try {
            // IsoDep卡片通信的工具类，Tag就是卡
            IsoDep isoDep = IsoDep.get((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
            if (isoDep == null) {
                String info = "读取卡信息失败";
                Log.i(TAG, "onNewIntent " + info);
                toast(info);
                return;
            }

            // NFC与卡进行连接
            isoDep.connect();
            Log.i(TAG, "onNewIntent isoDep connect");
            //转换指令为byte[]
            byte[] command = buildSelectApdu(SAMPLE_LOYALTY_CARD_AID);

            // 发送指令
            byte[] result = isoDep.transceive(command);

            // 截取响应数据
            int resultLength = result.length;
            byte[] statusWord = {result[resultLength - 2], result[resultLength - 1]};
            byte[] payload = Arrays.copyOf(result, resultLength - 2);
            // 检验响应数据
            if (Arrays.equals(SELECT_OK, statusWord)) {
                String accountNumber = new String(payload, "UTF-8");
                Log.e(TAG, "----> " + accountNumber);
                mTvView.setText(accountNumber);
            } else {
                String info = bytesToString(result);
                Log.e(TAG, "----> error" + info);
                mTvView.setText(info);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检测是否支持 NFC
     */
    private void nfcCheck() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            String info = "手机不支付NFC功能";
            toast(info);
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            String info = "手机NFC功能没有打开";
            toast(info);
            Intent setNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivity(setNfc);
        } else {
            String info = "手机NFC功能正常";
            toast(info);
        }
    }

    private byte[] stringToBytes(String s) {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("指令字符串长度必须为偶数 !!!");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[(i / 2)] = ((byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16)));
        }
        return data;
    }

    private String bytesToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte d : data) {
            sb.append(String.format("%02X", d));
        }
        return sb.toString();
    }


    private byte[] buildSelectApdu(String aid) {
        return stringToBytes(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    private void toast(String info) {
        Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
    }

}
