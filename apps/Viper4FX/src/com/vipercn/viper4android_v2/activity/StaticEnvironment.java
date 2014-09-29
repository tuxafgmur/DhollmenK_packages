package com.vipercn.viper4android_v2.activity;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class StaticEnvironment {

    private static boolean sEnvironmentInitialized;
    private static String sExternalStoragePath = "";
    private static String sV4aRoot = "";
    private static String sV4aKernelPath = "";
    private static String sV4aProfilePath = "";

    private static boolean checkWritable(String mFileName) {
        try {
            byte[] mBlank = new byte[16];
            FileOutputStream fosOutput = new FileOutputStream(mFileName);
            fosOutput.write(mBlank);
            fosOutput.flush();
            fosOutput.close();
            fosOutput = null;
            mBlank = null;
            return new File(mFileName).delete();
        } catch (FileNotFoundException e) {
            Log.w("ViPER4Android", "FileNotFoundException, msg = " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.w("ViPER4Android", "IOException, msg = " + e.getMessage());
            return false;
        }
    }

    public static boolean isEnvironmentInitialized() {
        return sEnvironmentInitialized;
    }

    public static void initEnvironment() {
	String mExternalStoragePathName = Environment.getExternalStorageDirectory().getAbsolutePath();
        sExternalStoragePath = mExternalStoragePathName.endsWith("/")
             ? mExternalStoragePathName : mExternalStoragePathName + "/";
	sV4aRoot = sExternalStoragePath + "ViPER4Android/";
	sV4aProfilePath = sV4aRoot + "Profile/";
	sV4aKernelPath = "/data/media/0/ViPER4Android/Kernel/";
        sEnvironmentInitialized = true;
    }

    public static String getExternalStoragePath() {
        return sExternalStoragePath;
    }

    public static String getV4aRootPath() {
        return sV4aRoot;
    }

    public static String getV4aKernelPath() {
        return sV4aKernelPath;
    }

    public static String getV4aProfilePath() {
        return sV4aProfilePath;
    }
}
