package com.yeyupiaoling.cv.paddle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import com.baidu.paddle.lite.MobileConfig;
import com.baidu.paddle.lite.PaddlePredictor;
import com.baidu.paddle.lite.PowerMode;
import com.baidu.paddle.lite.Tensor;
import com.yeyupiaoling.cv.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

public class PaddleDetection {
    private static final String TAG = PaddleDetection.class.getName();

    private PaddlePredictor paddlePredictor;
    private Tensor inputTensor;
    private long[] inputShape = new long[]{1, 3, 300, 300};
    private float[] inputMean = new float[]{127.5f, 127.5f, 127.5f};
    private float[] inputStd = new float[]{0.007843f, 0.007843f, 0.007843f};
    private final int NUM_THREADS = 4;
    private float SCORE_THRESHOLD = 0.45f;

    /**
     * @param modelPath model path
     */
    public PaddleDetection(String modelPath) throws Exception {
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new Exception("model file is not exists!");
        }
        try {
            MobileConfig config = new MobileConfig();
            config.setModelFromFile(modelPath);
            config.setThreads(NUM_THREADS);
            config.setPowerMode(PowerMode.LITE_POWER_HIGH);
            paddlePredictor = PaddlePredictor.createPaddlePredictor(config);

            inputTensor = paddlePredictor.getInput(0);
            inputTensor.resize(inputShape);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("load model fail!");
        }
    }

    public List<float[]> predictImage(String image_path) throws Exception {
        if (!new File(image_path).exists()) {
            throw new Exception("image file is not exists!");
        }
        FileInputStream fis = new FileInputStream(image_path);
        Bitmap bitmap = BitmapFactory.decodeStream(fis);
        return predictImage(bitmap);
    }

    public List<float[]> predictImage(Bitmap bitmap) throws Exception {
        return predict(bitmap);
    }


    // prediction
    private List<float[]> predict(Bitmap bmp) throws Exception {
        Bitmap b = getScaleBitmap(bmp);
        float[] inputData = getScaledMatrix(b, (int) inputShape[2], (int) inputShape[3]);
//        int imgWidth = bmp.getWidth();
//        int imgHeight = bmp.getHeight();
        b.recycle();
        bmp.recycle();
        inputTensor.setData(inputData);

        try {
            paddlePredictor.run();
        } catch (Exception e) {
            throw new Exception("predict image fail! log:" + e);
        }
        Tensor outputTensor = paddlePredictor.getOutput(0);

        float[] result = outputTensor.getFloatData();
        int length = result.length / 6;
        List<float[]> results = new ArrayList<>();

        for (int i = 0; i < length; i += 6) {
            float score = outputTensor.getFloatData()[i + 1];
            if (score < SCORE_THRESHOLD) {
                continue;
            }
            int label = (int) outputTensor.getFloatData()[i];
            float rawLeft = outputTensor.getFloatData()[i + 2];
            float rawTop = outputTensor.getFloatData()[i + 3];
            float rawRight = outputTensor.getFloatData()[i + 4];
            float rawBottom = outputTensor.getFloatData()[i + 5];
            float clampedLeft = Math.max(Math.min(rawLeft, 1.f), 0.f);
            float clampedTop = Math.max(Math.min(rawTop, 1.f), 0.f);
            float clampedRight = Math.max(Math.min(rawRight, 1.f), 0.f);
            float clampedBottom = Math.max(Math.min(rawBottom, 1.f), 0.f);
//            float imgLeft = clampedLeft * imgWidth;
//            float imgTop = clampedTop * imgWidth;
//            float imgRight = clampedRight * imgHeight;
//            float imgBottom = clampedBottom * imgHeight;
            float[] r = new float[]{label, score, clampedLeft, clampedTop, clampedRight, clampedBottom};
            results.add(r);
            Log.d(TAG, Arrays.toString(r));
        }
        return results;
    }

    // 对将要预测的图片进行预处理
    private float[] getScaledMatrix(Bitmap bitmap, int desWidth, int desHeight) {
        float[] dataBuf = new float[3 * desWidth * desHeight];
        int rIndex;
        int gIndex;
        int bIndex;
        int[] pixels = new int[desWidth * desHeight];
        Bitmap bm = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false);
        bm.getPixels(pixels, 0, desWidth, 0, 0, desWidth, desHeight);
        int j = 0;
        int k = 0;
        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            j = i / desHeight;
            k = i % desWidth;
            rIndex = j * desWidth + k;
            gIndex = rIndex + desHeight * desWidth;
            bIndex = gIndex + desHeight * desWidth;
            // 转成BGR通道顺序
            dataBuf[bIndex] = (((clr & 0x000000ff)) - inputMean[2]) * inputStd[2];
            dataBuf[gIndex] = (((clr & 0x0000ff00) >> 8) - inputMean[1]) * inputStd[1];
            dataBuf[rIndex] = (((clr & 0x00ff0000) >> 16) - inputMean[0]) * inputStd[0];
        }
        return dataBuf;
    }

    private Bitmap getScaleBitmap(Bitmap bitmap) {
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        int size = (int) inputShape[2];
        float scaleWidth = (float) size / bitmap.getWidth();
        float scaleHeight = (float) size / bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, true);
    }
}
