package com.tondz.phathiengianlanthicu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    CelebSDK yolov8Ncnn = new CelebSDK();
    private SurfaceView cameraView;
    private static final int REQUEST_CAMERA = 510;
    TextToSpeech textToSpeech;
    ImageView imgView;
    TextView tvName;
    Handler handler;
    Runnable runnable;
    private boolean canPlaySound = true;
    Button btnDoiCamera;
    private int facing = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();
        checkPermissions();
        reload();
        onClick();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                canPlaySound = true;
            }
        };
        getObject();

    }

    private void getObject() {
        new Thread(() -> {
            while (true) {
                String pose = yolov8Ncnn.getPose();
                if (!pose.isEmpty()) {
                    List<Double> poseList = getPose(pose);
                    double yaw = poseList.get(0);
                    double pitch = poseList.get(1);
                    double roll = poseList.get(2);
                    String direction = "";
                    if (yaw > 30) {
                        direction += "Quay trái, ";
                    } else if (yaw < -30) {
                        direction += "Quay phải, ";
                    }

                    if (pitch > 30) {
                        direction += "Nhìn lên, ";
                    }
                    if (!direction.isEmpty()) {
                        if (canPlaySound) {
                            speak("bạn đang " + direction);
                            canPlaySound = false;
                            handler.postDelayed(runnable, 3000);
                        }
                    }

                } else {
                    if (canPlaySound) {
                        speak("Không phát hiện khuôn mặt");
                        canPlaySound = false;
                        handler.postDelayed(runnable, 3000);
                    }
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void onClick() {
        btnDoiCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int new_facing = 1 - facing;
                yolov8Ncnn.closeCamera();
                yolov8Ncnn.openCamera(new_facing);
                facing = new_facing;
            }
        });

    }

    private List<Double> getPose(String pose) {
        List<Double> poseList = new ArrayList<>();
        String[] arr = pose.split(",");
        poseList.add(Double.parseDouble(arr[0]));
        poseList.add(Double.parseDouble(arr[1]));
        poseList.add(Double.parseDouble(arr[2]));
        return poseList;

    }

    private void speak(String noiDung) {
        textToSpeech.speak(noiDung, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void init() {
        btnDoiCamera = findViewById(R.id.btnChangeCamera);
        cameraView = findViewById(R.id.cameraview);
        cameraView.getHolder().addCallback(this);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.forLanguageTag("vi-VN"));
                }
            }
        });


    }

    private void reload() {
        boolean ret_init = yolov8Ncnn.loadModel(getAssets());
        if (!ret_init) {
            Log.e("NhanDienNguoiThanActivity", "yolov8ncnn loadModel failed");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        yolov8Ncnn.setOutputWindow(holder.getSurface());


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        yolov8Ncnn.openCamera(facing);
    }

    @Override
    public void onPause() {
        super.onPause();
        yolov8Ncnn.closeCamera();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }
}