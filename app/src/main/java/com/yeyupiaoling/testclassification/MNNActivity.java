
package com.yeyupiaoling.testclassification;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yeyupiaoling.testclassification.lite.TFLiteClassification;
import com.yeyupiaoling.testclassification.mnn.MNNClassification;
import com.yeyupiaoling.testclassification.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MNNActivity extends AppCompatActivity {
    private static final String TAG = MNNActivity.class.getName();
    private String classification_model_path;
    private String classification_quant_model_path;
    private MNNClassification mnnClassification;
    private TextView textView;
    private TextView logTextView;
    private ProgressBar progressBar;
    private boolean isInfer = false;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnn);
        classification_model_path = getCacheDir().getAbsolutePath() + File.separator + "mobilenet_v2.mnn";
        Utils.copyFileFromAsset(MNNActivity.this, "mobilenet_v2.mnn", classification_model_path);
        classification_quant_model_path = getCacheDir().getAbsolutePath() + File.separator + "mobilenet_v2_quant.mnn";
        Utils.copyFileFromAsset(MNNActivity.this, "mobilenet_v2_quant.mnn", classification_quant_model_path);
        initView();
        selectModelDialog();
    }

    private void initView(){
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        imageView = findViewById(R.id.image_view);
        textView = findViewById(R.id.text1);
        progressBar = findViewById(R.id.progress_bar);
        logTextView = findViewById(R.id.log_tv);
        logTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        //设置是否有返回图标
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInfer) {
                    imageView.setVisibility(View.GONE);
                    logTextView.setVisibility(View.VISIBLE);
                    logTextView.setText("");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            isInfer = true;
                            predict();
                            isInfer = true;
                        }
                    }).start();
                } else {
                    Toast.makeText(MNNActivity.this, "正在预测中，请勿重复点击按钮", Toast.LENGTH_SHORT).show();
                }
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInfer) {
                    imageView.setVisibility(View.VISIBLE);
                    logTextView.setVisibility(View.GONE);
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1);
                } else {
                    Toast.makeText(MNNActivity.this, "正在预测中，请勿重复点击按钮", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void selectModelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择模型");
        builder.setMessage("是否加载量化模型？");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mnnClassification = new MNNClassification(classification_quant_model_path);
                    Toast.makeText(MNNActivity.this, "模型加载成功！", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MNNActivity.this, "模型加载失败！", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    finish();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mnnClassification = new MNNClassification(classification_model_path);
                    Toast.makeText(MNNActivity.this, "模型加载成功！", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MNNActivity.this, "模型加载失败！", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    finish();
                }
            }
        });
        builder.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String image_path;
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                if (data == null) {
                    Log.w("onActivityResult", "user photo data is null");
                    return;
                }
                Uri image_uri = data.getData();
                // get image path from uri
                image_path = Utils.getPathFromURI(MNNActivity.this, image_uri);
                // predict image
                try {
                    FileInputStream fis = new FileInputStream(image_path);
                    imageView.setImageBitmap(BitmapFactory.decodeStream(fis));
                    int result = mnnClassification.predictImage(image_path);
                    Log.d(TAG, "预测结果: " + result);
                    textView.setText("预测结果: " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void predict() {
        try {
            AssetManager assetManager = getApplicationContext().getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("test_list.txt")));
            Map<String, Integer> data = new HashMap<>();
            String readLine;
            while ((readLine = reader.readLine()) != null) {
                String[] d = readLine.split("\t");
                String image_path = Environment.getExternalStorageDirectory() + "/" + d[0];
                int id = Integer.valueOf(d[1]);
                data.put(image_path, id);
            }
            reader.close();
            int sum = data.size();
            int accuracy_sum = 0;
            int i = 0;
            progressBar.setMax(sum);
            long start_time = System.currentTimeMillis();
            for (final String path : data.keySet()) {
                final int result = mnnClassification.predictImage(path);
                final int really = data.get(path);
                // output log
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String[] paths = path.split("/");
                        String text = paths[paths.length - 1].substring(10) + " —— label: " + result + ", really: " + really;
                        Log.d(TAG, text);
                        logTextView.append(text);
                        logTextView.append("\n");
                        int offset = logTextView.getLineCount() * logTextView.getLineHeight();
                        if (offset > logTextView.getHeight()) {
                            logTextView.scrollTo(0, offset - logTextView.getHeight());
                        }
                    }
                });
                // calculate accuracy
                if (result == really) {
                    accuracy_sum++;
                }
                progressBar.setProgress(i++);
            }
            long end_time = System.currentTimeMillis();
            final String text = String.format("图片总数: %d, 准确率: %.3f, 预测时间: %dms",
                    sum, (float) accuracy_sum / sum, (end_time - start_time) / sum);
            Log.d(TAG, text);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                    logTextView.append(text);
                    logTextView.append("\n");
                    int offset = logTextView.getLineCount() * logTextView.getLineHeight();
                    if (offset > logTextView.getHeight()) {
                        logTextView.scrollTo(0, offset - logTextView.getHeight());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        mnnClassification.release();
        super.onDestroy();
    }
}
