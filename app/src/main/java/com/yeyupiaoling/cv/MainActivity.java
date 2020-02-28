package com.yeyupiaoling.cv;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasPermission()){
            requestPermission();
        }
        initView();
    }

    private void initView(){
        findViewById(R.id.tflite_btn).setOnClickListener(this);
        findViewById(R.id.mnn_btn).setOnClickListener(this);
        findViewById(R.id.paddle_btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (hasPermission()){
            switch (view.getId()) {
                case R.id.tflite_btn:
                    Intent intent1 = new Intent(MainActivity.this, TFLiteClassificationActivity.class);
                    startActivity(intent1);
                    break;
                case R.id.mnn_btn:
                    Intent intent2 = new Intent(MainActivity.this, MNNClassificationActivity.class);
                    startActivity(intent2);
                    break;
                case R.id.paddle_btn:
                    Intent intent4 = new Intent(MainActivity.this, PaddleClassificationActivity.class);
                    startActivity(intent4);
                    break;
            }
        }else {
            Toast.makeText(MainActivity.this, "你还没有授权！", Toast.LENGTH_SHORT).show();
            requestPermission();
        }
    }

    // check had permission
    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    // request permission
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}
