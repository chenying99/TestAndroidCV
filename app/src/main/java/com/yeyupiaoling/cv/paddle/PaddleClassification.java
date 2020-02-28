package com.yeyupiaoling.cv.paddle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.baidu.paddle.lite.MobileConfig;
import com.baidu.paddle.lite.PaddlePredictor;
import com.baidu.paddle.lite.PowerMode;
import com.baidu.paddle.lite.Tensor;
import com.yeyupiaoling.cv.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

public class PaddleClassification {
    private static final String TAG = PaddleClassification.class.getName();

    private PaddlePredictor paddlePredictor;
    private Tensor inputTensor;
    private long[] inputShape = new long[]{1, 3, 224, 224};
    private static float[] inputMean = new float[]{128.0f, 128.0f, 128.0f};
    private static float[] inputStd = new float[]{128.0f, 128.0f, 128.0f};
    private static final int NUM_THREADS = 4;

    /**
     * @param modelPath model path
     */
    public PaddleClassification(String modelPath) throws Exception {
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

    public int predictImage(String image_path) throws Exception {
        if (!new File(image_path).exists()) {
            throw new Exception("image file is not exists!");
        }
        FileInputStream fis = new FileInputStream(image_path);
        Bitmap bitmap = BitmapFactory.decodeStream(fis);
        int result = predictImage(bitmap);
        if (bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return result;
    }

    public int predictImage(Bitmap bitmap) throws Exception {
        return predict(bitmap);
    }


    // prediction
    private int predict(Bitmap bmp) throws Exception {
        Bitmap b = getScaleBitmap(bmp);
        float[] inputData = getScaledMatrix(b, (int) inputShape[2], (int) inputShape[3]);
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
        Log.d(TAG, Arrays.toString(result));
        return Utils.getMaxResult(result);
    }

    // 对将要预测的图片进行预处理
    public static float[] getScaledMatrix(Bitmap bitmap, int desWidth, int desHeight) {
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
            // 转成RGB通道顺序
            dataBuf[rIndex] = (((clr & 0x00ff0000) >> 16) - inputMean[0]) / inputStd[1];
            dataBuf[gIndex] = (((clr & 0x0000ff00) >> 8) - inputMean[1]) / inputStd[1];
            dataBuf[bIndex] = (((clr & 0x000000ff)) - inputMean[2]) / inputStd[1];

        }
        if (bm.isRecycled()) {
            bm.recycle();
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
