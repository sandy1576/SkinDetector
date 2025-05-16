package com.example.skincancer.activities;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Classifier {
    private Interpreter INTERPRETER;
    private List<String> LABEL_LIST;
    private final int INPUT_SIZE;
    private static final int PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255.0f;
    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.4f;

    public static class Recognition {
        final String id;
        final String title;
        final float confidence;

        public Recognition(String id, String title, float confidence) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public float getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            return "Title = " + title + ", Confidence = " + confidence;
        }
    }

    public Classifier(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException {
        this.INPUT_SIZE = inputSize;
        this.INTERPRETER = new Interpreter(loadModelFile(assetManager, modelPath));
        this.LABEL_LIST = loadLabelList(assetManager, labelPath);
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(assetManager.openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                assetManager.openFd(modelPath).getStartOffset(),
                assetManager.openFd(modelPath).getDeclaredLength());
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labels = new ArrayList<>();
        InputStream inputStream = assetManager.open(labelPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }

    public List<Recognition> recognizeImage(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
        float[][] result = new float[1][LABEL_LIST.size()];
        INTERPRETER.run(byteBuffer, result);
        return getSortedResult(result);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < INPUT_SIZE; j++) {
                int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16 & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat(((val >> 8 & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        return byteBuffer;
    }

    private List<Recognition> getSortedResult(float[][] labelProbArray) {
        Log.d("Classifier", "List Size: (" + labelProbArray.length + ", " + labelProbArray[0].length + ", " + LABEL_LIST.size() + ")");

        PriorityQueue<Recognition> pq = new PriorityQueue<>(MAX_RESULTS,
                new Comparator<Recognition>() {
                    @Override
                    public int compare(Recognition r1, Recognition r2) {
                        return Float.compare(r2.confidence, r1.confidence);
                    }
                });

        for (int i = 0; i < LABEL_LIST.size(); i++) {
            float confidence = labelProbArray[0][i];
            if (confidence >= THRESHOLD) {
                pq.add(new Recognition(String.valueOf(i), LABEL_LIST.size() > i ? LABEL_LIST.get(i) : "Unknown", confidence));
            }
        }

        Log.d("Classifier", "pqsize: (" + pq.size() + ")");

        List<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; i++) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }
}

