package com.example.landmarkclassification.landmark;

import android.util.Log;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;

import java.io.File;


public class FaceDetSingleton {
    private static final String TAG = "FaceDetSingelton";
    private static FaceDet faceDet;


    private FaceDetSingleton() {
    }


    /**
     * instantiation of FaceDet is pretty expensive (5 Seconds) so we make sure we dont have to do it twice
     *
     * @return FaceDet instance which can be globaly used
     */
    public static FaceDet getInstance() {
        String targetPath = Constants.getFaceShapeModelPath();
        //lazy initialization
        if (faceDet == null) {
            //ToDo double checked lock semse outdated
            //double checked lock for thread saftey
            synchronized (FaceDetSingleton.class) {
                if (faceDet == null) {
                    Log.i(TAG, "landmarks.dat exists: " + new File(targetPath).exists());
                    if (new File(targetPath).exists()) {
                        Log.i(TAG, "Create Facedet");
                        faceDet = new FaceDet(Constants.getFaceShapeModelPath());
                        Log.i(TAG, "Facedet created");
                        return faceDet;
                    } else {
                        Log.d(TAG, "landsmarks.dat dont exists. Check Read- and Writepermissions");
                        faceDet = null;
                        return null;
                    }
                }
            }
        }
        Log.i(TAG, "FaceDet already created so return instance");
        return faceDet;
    }

}

