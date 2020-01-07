/*
 *  Copyright (C) 2020-present TrueBypass
 */

package com.example.landmarkclassification;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.landmarkclassification.Util.FileUtils;
import com.tzutalin.dlib.Constants;
import com.example.landmarkclassification.GUI.CameraConnectionFragment;
import com.example.landmarkclassification.landmark.FaceDetSingleton;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final String TAG = "MainActivity";
    private static int OVERLAY_PERMISSION_REQ_CODE = 1;


    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }



        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if (verifyPermissions(this)) {
                setContentView(R.layout.activity_camera);
                Log.i(TAG, "GOT PERMISSON");
                if (new File(Constants.getFaceShapeModelPath()).exists()) {
                    Log.i(TAG, "landmarks.dat already exists so create FaceDet");
                    new Runnable() {
                        @Override
                        public void run() {
                            FaceDetSingleton.getInstance();
                        }
                    }.run();
                }
            }
            ;
        }

        if (null == savedInstanceState) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, CameraConnectionFragment.newInstance())
                    .commit();
        }

        setContentView(R.layout.activity_camera);



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "No Permission for: " + permissions[i]);
            } else {
                Log.i(TAG, "Got Permissions for: " + permissions[i]);
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.i(TAG, "GOT PERMISSON");
                    if (new File(Constants.getFaceShapeModelPath()).exists()) {
                        Log.i(TAG, "landmarks.dat already exists so create FaceDet");
                        new Runnable() {
                            @Override
                            public void run() {
                                FaceDetSingleton.getInstance();
                            }
                        }.run();
                    }else{
                        if(FileUtils.copyFileFromRawToOthers(this.getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath())){
                            Log.i(TAG, "Landmark.dat saved entering FacedetSingleton");
                            new Runnable() {
                                @Override
                                public void run() {
                                    FaceDetSingleton.getInstance();
                                }
                            }.run();
                        };
                    }
                }
            }
        }
    }


    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */

    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                    Toast.makeText(MainActivity.this, "CameraActivity\", \"SYSTEM_ALERT_WINDOW, permission not granted...", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        }
    }

}
