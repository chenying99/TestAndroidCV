package com.yeyupiaoling.testclassification.lite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.yeyupiaoling.testclassification.utils.Utils;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class TFLiteClassification {
    private static final String TAG = TFLiteClassification.class.getName();
    private Interpreter tflite;
    private final int[] imageShape;
    private TensorImage inputImageBuffer;
    private final TensorBuffer outputProbabilityBuffer;
    private static final int NUM_THREADS = 4;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private final ImageProcessor imageProcessor;


    /**
     * @param modelPath model path
     */
    public TFLiteClassification(String modelPath) throws Exception {


        File file = new File(modelPath);
        if (!file.exists()) {
            throw new Exception("model file is not exists!");
        }
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(NUM_THREADS);
//            NnApiDelegate delegate = new NnApiDelegate();
//            GpuDelegate delegate = new GpuDelegate();
//            options.addDelegate(delegate);
            tflite = new Interpreter(file, options);
            // {1, height, width, 3}
            imageShape = tflite.getInputTensor(0).shape();
            DataType imageDataType = tflite.getInputTensor(0).dataType();
            inputImageBuffer = new TensorImage(imageDataType);
            // {1, NUM_CLASSES}
            int[] probabilityShape = tflite.getOutputTensor(0).shape();
            DataType probabilityDataType = tflite.getOutputTensor(0).dataType();
            outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("load model fail!");
        }
        // Creates processor for the TensorImage.
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(imageShape[1], imageShape[2], ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();
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
        inputImageBuffer = loadImage(bmp);

        try {
            tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
        } catch (Exception e) {
            throw new Exception("predict image fail! log:" + e);
        }

        float[] results = outputProbabilityBuffer.getFloatArray();
        Log.d(TAG, Arrays.toString(results));
        return Utils.getMaxResult(results);
    }


    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);
        return imageProcessor.process(inputImageBuffer);
    }

    public void close(){
        tflite.close();
    }
}
