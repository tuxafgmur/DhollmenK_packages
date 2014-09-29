package com.vipercn.viper4android_v2.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;

import com.vipercn.viper4android_v2.R;
import com.vipercn.viper4android_v2.service.ViPER4AndroidService;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

public class Utils {

    public class AudioEffectUtils {

        private AudioEffect.Descriptor[] mAudioEffectList;
        private boolean mHasViPER4AndroidEngine;
        private final int[] mV4AEngineVersion = new int[4];

        public AudioEffectUtils() {
            try {
                mAudioEffectList = AudioEffect.queryEffects();
            } catch (Exception e) {
                mAudioEffectList = null;
                mHasViPER4AndroidEngine = false;
                mV4AEngineVersion[0] = 0;
                mV4AEngineVersion[1] = 0;
                mV4AEngineVersion[2] = 0;
                mV4AEngineVersion[3] = 0;
                Log.e("ViPER4A", "Failed to query audio effects");
                return;
            }
            if (mAudioEffectList == null) {
                mHasViPER4AndroidEngine = false;
                mV4AEngineVersion[0] = 0;
                mV4AEngineVersion[1] = 0;
                mV4AEngineVersion[2] = 0;
                mV4AEngineVersion[3] = 0;
                Log.e("ViPER4", "Failed to query audio effects");
                return;
            }

            AudioEffect.Descriptor mViper4AndroidEngine = null;
            for (int i = 0; i < mAudioEffectList.length; i++) {
                if (mAudioEffectList[i] == null) continue;
                try {
                    AudioEffect.Descriptor aeEffect = mAudioEffectList[i];
                    if (aeEffect.uuid.equals(ViPER4AndroidService.ID_V4A_GENERAL_FX)) {
                        mViper4AndroidEngine = aeEffect;
                    }
                } catch (Exception e) {
                    Log.e("ViPER4", "AudioEffect descriptor error , msg = " + e.getMessage());
                }
            }

            if (mViper4AndroidEngine == null) {
                Log.e("ViPER4", "Engine not found");
                mHasViPER4AndroidEngine = false;
                mV4AEngineVersion[0] = 0;
                mV4AEngineVersion[1] = 0;
                mV4AEngineVersion[2] = 0;
                mV4AEngineVersion[3] = 0;
                return;
            }

            // Extract engine version
            try {
                String v4aVersionLine = mViper4AndroidEngine.name;
                if (v4aVersionLine.contains("[") && v4aVersionLine.contains("]")) {
                    if (v4aVersionLine.length() >= 23) {
                        // v4aVersionLine should be "ViPER4Android [A.B.C.D]"
                        v4aVersionLine = v4aVersionLine.substring(15);
                        while (v4aVersionLine.endsWith("]"))
                            v4aVersionLine = v4aVersionLine.substring(0, v4aVersionLine.length() - 1);
                        // v4aVersionLine should be "A.B.C.D"
                        String[] mVerBlocks = v4aVersionLine.split("\\.");
                        if (mVerBlocks.length == 4) {
                            mV4AEngineVersion[0] = Integer.parseInt(mVerBlocks[0]);
                            mV4AEngineVersion[1] = Integer.parseInt(mVerBlocks[1]);
                            mV4AEngineVersion[2] = Integer.parseInt(mVerBlocks[2]);
                            mV4AEngineVersion[3] = Integer.parseInt(mVerBlocks[3]);
                        }
                        mHasViPER4AndroidEngine = true;
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e("ViPER4", "Engine version exception: " + e.getMessage());
            }

	    if (! SystemProperties.get("ro.product.rkg").equals("1")) {
		mHasViPER4AndroidEngine = false;
		return;
	    }

            Log.e("ViPER4", "Can not extract engine version");
            mHasViPER4AndroidEngine = false;
            mV4AEngineVersion[0] = 0;
            mV4AEngineVersion[1] = 0;
            mV4AEngineVersion[2] = 0;
            mV4AEngineVersion[3] = 0;
        }

        public AudioEffect.Descriptor[] getAudioEffectList() {
            return mAudioEffectList;
        }

        public boolean isViPER4AndroidEngineFound() {
            return mHasViPER4AndroidEngine;
        }

        public int[] getViper4AndroidEngineVersion() {
            return mV4AEngineVersion;
        }
    }

    public static class CpuInfo {

        private boolean mCpuHasNEON;
        private boolean mCpuHasVFP;

        // Lets read /proc/cpuinfo in java
        private boolean readCpuInfo() {
            String mCPUInfoFile = "/proc/cpuinfo";
            FileReader cpuInfoReader = null;
            BufferedReader bufferReader = null;

            mCpuHasNEON = false;
            mCpuHasVFP = false;

            // Find "Features" line, extract neon and vfp
            try {
                cpuInfoReader = new FileReader(mCPUInfoFile);
                bufferReader = new BufferedReader(cpuInfoReader);
                while (true) {
                    String mLine = bufferReader.readLine();
                    if (mLine == null) {
                        break;
                    }
                    mLine = mLine.trim();
                    if (mLine.startsWith("Features")) {
                        StringTokenizer stBlock = new StringTokenizer(mLine);
                        while (stBlock.hasMoreElements()) {
                            String mFeature = stBlock.nextToken();
                            if (mFeature != null) {
                                if (mFeature.equalsIgnoreCase("neon")) {
                                    mCpuHasNEON = true;
                                } else if (mFeature.equalsIgnoreCase("vfp")) {
                                    mCpuHasVFP = true;
                                }
                            }
                        }
                    }
                }
                bufferReader.close();
                cpuInfoReader.close();
                bufferReader = null;
                cpuInfoReader = null;

                return !(!mCpuHasNEON && !mCpuHasVFP);
            } catch (IOException e) {
                try {
                    if (bufferReader != null) {
                        bufferReader.close();
                    }
                    if (cpuInfoReader != null) {
                        cpuInfoReader.close();
                    }
                    bufferReader = null;
                    cpuInfoReader = null;
                    return false;
                } catch (Exception ex) {
                    bufferReader = null;
                    cpuInfoReader = null;
                    return false;
                }
            }
        }

        // Lets read /proc/cpuinfo in jni
        private void readCPUInfoJni() {
            mCpuHasNEON = V4AJniInterface.IsCPUSupportNEON();
            mCpuHasVFP = V4AJniInterface.IsCPUSupportVFP();
        }

        // Buffered result
        public CpuInfo() {
            mCpuHasNEON = false;
            mCpuHasVFP = false;
            if (!readCpuInfo()) {
                readCPUInfoJni();
            }
        }

        public boolean hasNEON() {
            return mCpuHasNEON;
        }

        public boolean hasVFP() {
            return mCpuHasVFP;
        }
    }

    // Check if Busybox is installed & offer installation if not found
    public static boolean isBusyBoxInstalled(Context ctx) {
        return RootTools.isBusyboxAvailable();
    }

    // Get a file length
    private static long getFileLength(String mFileName) {
        if (!(new File(mFileName).isFile())) return 0;
        return new File(mFileName).length();
    }

    // Read file list from path
    public static void getFileNameList(File path, String fileExt, ArrayList<String> fileList) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (null == files) return;
            for (File file : files) getFileNameList(file, fileExt, fileList);
        } else {
            String filePath = path.getAbsolutePath();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (fileName.toLowerCase(Locale.US).endsWith(fileExt))
                fileList.add(fileName);
        }
    }

    // Get profile name from a file
    private static String getProfileName(String mProfileFileName) {
        try {
            FileInputStream fisInput = new FileInputStream(mProfileFileName);
            InputStreamReader isrInput = new InputStreamReader(fisInput, "UTF-8");
            BufferedReader bufferInput = new BufferedReader(isrInput);
            String mProfileName = "";
            while (true) {
                String mLine = bufferInput.readLine();
                if (mLine == null) break;
                if (mLine.startsWith("#")) continue;

                String[] mChunks = mLine.split("=");
                if (mChunks.length != 2) continue;
                if (mChunks[0].trim().equalsIgnoreCase("profile_name")) {
                    mProfileName = mChunks[1];
                    break;
                }
            }
            bufferInput.close();
            isrInput.close();
            fisInput.close();
            bufferInput = null;
            isrInput = null;
            fisInput = null;
            return mProfileName;
        } catch (Exception e) {
            return "";
        }
    }

    // Get profile name list
    public static ArrayList<String> getProfileList(String mProfileDir) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> profileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", profileList);

            ArrayList<String> mProfileNameList = new ArrayList<String>();
            for (String mProfileList : profileList) {
                String mFileName = mProfileDir + mProfileList;
                String mName = getProfileName(mFileName);
                mProfileNameList.add(mName.trim());
            }
            profileList = null;

            return mProfileNameList;
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    // Load profile from file
    public static boolean loadProfileV1(String mProfileName, String mProfileDir,
            String mPreferenceName, Context ctx) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> profileFileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", profileFileList);
            String mProfileFileName = "";
            for (String mProfileFileList : profileFileList) {
                String mFileName = mProfileDir + mProfileFileList;
                String mName = getProfileName(mFileName);
                if (mProfileName.trim().equalsIgnoreCase(mName.trim())) {
                    mProfileFileName = mFileName;
                    break;
                }
            }
            if (mProfileFileName.equals("")) return false;

            SharedPreferences preferences = ctx.getSharedPreferences(mPreferenceName,
                    Context.MODE_PRIVATE);
            if (preferences != null) {
                FileInputStream fisInput = new FileInputStream(mProfileFileName);
                InputStreamReader isrInput = new InputStreamReader(fisInput, "UTF-8");
                BufferedReader bufferInput = new BufferedReader(isrInput);
                Editor e = preferences.edit();
                while (true) {
                    String mLine = bufferInput.readLine();
                    if (mLine == null) break;
                    if (mLine.startsWith("#")) continue;

                    String[] mChunks = mLine.split("=");
                    if (mChunks.length != 3) continue;
                    if (mChunks[1].trim().equalsIgnoreCase("boolean")) {
                        String mParameter = mChunks[0];
                        boolean mValue = Boolean.valueOf(mChunks[2]);
                        e.putBoolean(mParameter, mValue);
                    } else if (mChunks[1].trim().equalsIgnoreCase("string")) {
                        String mParameter = mChunks[0];
                        String mValue = mChunks[2];
                        e.putString(mParameter, mValue);
                    } else {
                    }
                }
                e.commit();
                bufferInput.close();
                isrInput.close();
                fisInput.close();
                bufferInput = null;
                isrInput = null;
                fisInput = null;
                return true;
            } else
                return false;
        } catch (Exception e) {
            Log.e("ViPER4", "loadProfile Error: " + e.getMessage());
            return false;
        }
    }

    // Get application data path
    public static String getBasePath(Context ctx) {
        Context mContext = ctx.getApplicationContext();
        String mBasePath = "";
        if (mContext != null) {
            // No try catch the mContext != null will prevent a possible NPE here
            if (mContext.getFilesDir().exists()) {
                mBasePath = mContext.getFilesDir().getAbsolutePath();
            } else if (!mContext.getFilesDir().mkdirs()) {
                mBasePath = "";
            }
        } else {
            mBasePath = "";
        }
        return mBasePath;
    }

    // Copy assets to local
    public static boolean copyAssetsToLocal(Context ctx, String mSourceName, String mDestinationName) {
        String mBasePath = getBasePath(ctx);
        if (mBasePath.equals("")) return false;
        mDestinationName = mBasePath + "/" + mDestinationName;

        InputStream myInput;
        OutputStream myOutput;
        String outFileName = mDestinationName;
        try {
            File hfOutput = new File(mDestinationName);
            if (hfOutput.exists()) hfOutput.delete();
            myOutput = new FileOutputStream(outFileName);
            myInput = ctx.getAssets().open(mSourceName);
            byte[] buffer = new byte[4096]; /* 4K page size */
            int length;
            while ((length = myInput.read(buffer)) > 0)
                myOutput.write(buffer, 0, length);
            myOutput.flush();
            myInput.close();
            myOutput.close();
            buffer = null;
            myInput = null;
            myOutput = null;
        } catch (Exception e) {
            Log.e("ViPER4", "Copy assets to local failed, msg = " + e.getMessage());
            return false;
        }

        return true;
    }

    // Uninstall ViPER4Android FX driver
    public static void uninstallDrv_FX() {

        // Lets acquire root first :)
        if (!RootTools.isAccessGiven()) {
            return;
        }

        // Then delete the driver
        String mDriverPathName = "/system/lib/soundfx/libv4a_fx.so";
        try {
            if (RootTools.exists(mDriverPathName)) {
                RootTools.deleteFileOrDirectory(mDriverPathName, true);
            }
            RootTools.closeAllShells();
        } catch (IOException e) {
            Log.e("ViPER4", "Driver uninstall failed, msg = " + e.getMessage());
        }
    }

    /*
     * Driver installation return value:
     * 0: Success
     * 1: Acquire root failed
     * 2: External storage not mounted
     * 3: I/O error
     * 4: Unsupported audio_config.conf file format
     * 5: Busybox not found
     * 6: Unknow error
     */

    // Install ViPER4Android FX driver through roottools
    private static int installDrv_FX_RootTools(Context ctx, String mDriverName) {
        boolean isAddondSupported = false;

        // Make sure we can use external storage for temp directory
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return 2;

        // Copy driver assets to local
        if (!copyAssetsToLocal(ctx, mDriverName, "libv4a_fx.so"))
            return 3;

        // Lets acquire root first :)
        if (!RootTools.isAccessGiven()) return 1;

        // Check chmod utils
        String mChmod;
        if (RootTools.checkUtil("chmod"))
            mChmod = "chmod";
        else {
            if (RootTools.checkUtil("busybox") && RootTools.hasUtil("chmod", "busybox"))
                mChmod = "busybox chmod";
            else if (RootTools.checkUtil("toolbox") && RootTools.hasUtil("chmod", "toolbox"))
                mChmod = "toolbox chmod";
            else
                return 5;
            }

        // Copy back to system
        boolean operationSuccess;
        String mBaseDrvPathName = getBasePath(ctx);
        String mAddondScriptPathName = mBaseDrvPathName;
        if (mBaseDrvPathName.endsWith("/")) {
            mBaseDrvPathName = mBaseDrvPathName + "libv4a_fx.so";
        } else {
            mBaseDrvPathName = mBaseDrvPathName + "/libv4a_fx.so";
        }
        try {
   	    // Copy files
	    operationSuccess = RootTools.remount("/system", "RW");
	    if (operationSuccess) {
		operationSuccess = RootTools.copyFile(mBaseDrvPathName,
			"/system/lib/soundfx/libv4a_fx.so", false, false);
	    }
	    // Modify permission
	    CommandCapture ccSetPermission = new CommandCapture(0,
		mChmod + " 644 /system/lib/soundfx/libv4a_fx.so");
	    RootTools.getShell(true).add(ccSetPermission);

	    RootTools.remount("/system", "RO");

            } catch (Exception e) {
                operationSuccess = false;
                Log.e("ViPER4", "Copy back to /system failed, msg = " + e.getMessage());
        }

        /* Cleanup temp file(s) and close root shell */
        try {
            // Close all shells
            RootTools.closeAllShells();
        } catch (Exception e) {
            if (!operationSuccess) {
            	return 6;
            } else {
            	return 0;
            }
        }

        return 0;
    }

    // Install ViPER4Android FX driver using without shell command method
    private static int installDrv_FX_WithoutShell(Context ctx, String mDriverName) {
        boolean isAddondSupported = false;
        int mShellCmdReturn;

        // Make sure we can use external storage for temp directory
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return 2;
        }

        // Copy driver assets to local
        if (!copyAssetsToLocal(ctx, mDriverName, "libv4a_fx.so")) {
            return 3;
        }

        // Copy back to system
        String mBaseDrvPathName = getBasePath(ctx);
        String mAddondScriptPathName = mBaseDrvPathName;
        if (mBaseDrvPathName.endsWith("/")) {
            mBaseDrvPathName = mBaseDrvPathName + "libv4a_fx.so";
        } else {
            mBaseDrvPathName = mBaseDrvPathName + "/libv4a_fx.so";
        }

	mShellCmdReturn = ShellCommand.rootExecuteWithoutShell("mount -o rw,remount /system");
	if (mShellCmdReturn != 0) {
	    Log.e("ViPER4", "Cannot remount /system");
	    return 5;
	}

	mShellCmdReturn = ShellCommand
		.rootExecuteWithoutShell("rm /system/lib/soundfx/libv4a_fx.so");
	if (mShellCmdReturn != 0) {
	    Log.e("ViPER4", "Can not remove driver from /system");
	    return 5;
	}

	mShellCmdReturn = ShellCommand.rootExecuteWithoutShell("cp " + mBaseDrvPathName
		+ " /system/lib/soundfx/libv4a_fx.so");
	if (mShellCmdReturn != 0) {
	    Log.e("ViPER4", "Can not copy driver to /system");
	    return 5;
	}

	mShellCmdReturn = ShellCommand
		.rootExecuteWithoutShell("chmod 644 /system/lib/soundfx/libv4a_fx.so");
	if (mShellCmdReturn != 0) {
	    Log.e("ViPER4", "Can not change driver's permission");
	    return 5;
	}

	mShellCmdReturn = ShellCommand.rootExecuteWithoutShell("sync");

	mShellCmdReturn = ShellCommand.rootExecuteWithoutShell("mount -o ro,remount /system");

        if (RootTools.exists("/system/lib/soundfx/libv4a_fx.so")) {
            return 0;
        } else {
	    return 6;
        }
    }

    // Install ViPER4Android FX driver
    public static int installDrv_FX(Context ctx, String mDriverName) {
    	// Try install driver using RootTools
    	int method1Result = installDrv_FX_RootTools(ctx, mDriverName);
    	switch (method1Result) {
    	case 0:  // Success
    	case 1:  // Acquire root failed
    	case 2:  // External storage not mounted
    	case 3:  // I/O error
    	case 4:  // Unsupported audio_config.conf file format
    		return method1Result;  // Report result to user
    	case 5:  // Busybox not found
    	case 6:  // Unknow error
    		/* TODO: implement method2.
    		 * Maybe we can use a shell script to install the driver.
    		 * */
    		return 6;
    	}

    	return 6;
    }

    /**
     * Restart the activity smoothly
     *
     * @param activity
     */
    public static void restartActivity(final Activity activity) {
        if (activity == null) return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }
}
