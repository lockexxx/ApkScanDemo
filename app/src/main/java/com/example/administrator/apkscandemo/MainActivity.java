package com.example.administrator.apkscandemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.scanengine.ApkScanTask;
import com.example.scanengine.IScanTaskCallback;
import com.example.scanengine.TaskBus;

public class MainActivity extends Activity {
    private TaskBus mTaskBus = null;
    private TextView mTextView = null;
    private Button mButton = null;

    protected Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (!onMessage(msg)) {
                super.handleMessage(msg);
            }
        }
    };

    protected boolean onMessage(Message msg) {
        switch (msg.what) {
            case ApkScanTask.TASK_SCAN_RUNNING:
                Log.e("APKScan", "TASK_SCAN_RUNNING");
                break;
            case ApkScanTask.TASK_SCAN_FINISHED:
                Log.e("APKScan", "TASK_SCAN_FINISHED");
                break;
            case ApkScanTask.TASK_SCAN_FAILED:
                Log.e("APKScan", "TASK_SCAN_FAILED");
                break;
            case ApkScanTask.TASK_SCAN_UPDATE_DIRS:
                Log.e("APKScan", "TASK_SCAN_UPDATE_DIRS");
                break;
            case ApkScanTask.TASK_SCAN_UPDATE_MODEL:
                Log.e("APKScan", "TASK_SCAN_UPDATE_MODEL");
                break;
            default: /* NONE */
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView);
        mButton = (Button) findViewById(R.id.button);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
    }

    private void scan() {
        ApkScanTask apkScanTask = new ApkScanTask(this);
        apkScanTask.bindCallbackObj(new IScanTaskCallback() {
            @Override
            public void onMessage(int what, Object obj) {
                mHandler.sendMessage(mHandler.obtainMessage(what, obj));
            }
        });

        mTaskBus = new TaskBus();
        mTaskBus.pushTask(apkScanTask);
        mTaskBus.startScan();
    }
}
