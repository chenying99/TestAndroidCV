package com.yeyupiaoling.cv.mnn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.yeyupiaoling.cv.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class MNNClassification {
    private static final String TAG = MNNClassification.class.getName();

    private MNNNetInstance mNetInstance;
    private MNNNetInstance.Session mSession;
    private MNNNetInstance.Session.Tensor mInputTensor;
    private final MNNImageProcess.Config dataConfig;
    private Matrix imgData;
    private final int inputWidth = 224;
    private final int inputHeight = 224;
    private static final int NUM_THREADS = 4;

    /**
     * @param modelPath model path
     */
    public MNNClassification(String modelPath) throws Exception {
        dataConfig = new MNNImageProcess.Config();
        dataConfig.mean = new float[]{128.0f, 128.0f, 128.0f};
        dataConfig.normal = new float[]{0.0078125f, 0.0078125f, 0.0078125f};
        dataConfig.dest = MNNImageProcess.Format.RGB;
        imgData = new Matrix();

        File file = new File(modelPath);
        if (!file.exists()) {
            throw new Exception("model file is not exists!");
        }
        try {
            mNetInstance = MNNNetInstance.createFromFile(modelPath);
            MNNNetInstance.Config config = new MNNNetInstance.Config();
            config.numThread = NUM_THREADS;
            config.forwardType = MNNForwardType.FORWARD_CPU.type;
            mSession = mNetInstance.createSession(config);
            mInputTensor = mSession.getInput(null);
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
        imgData.reset();
        imgData.postScale(inputWidth / (float) bmp.getWidth(), inputHeight / (float) bmp.getHeight());
        imgData.invert(imgData);
        MNNImageProcess.convertBitmap(bmp, mInputTensor, dataConfig, imgData);

        try {
            mSession.run();
        } catch (Exception e) {
            throw new Exception("predict image fail! log:" + e);
        }
        MNNNetInstance.Session.Tensor output = mSession.getOutput(null);
        float[] result = output.getFloatData();
        Log.d(TAG, Arrays.toString(result));
        return Utils.getMaxResult(result);
    }

    public void release(){
        if (mNetInstance != null) {
            mNetInstance.release();
            mNetInstance = null;
        }
    }
}
