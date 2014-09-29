
package com.vipercn.viper4android_v2.activity;

import android.util.Log;

import java.io.IOException;

class ShellCommand {

    public static int rootExecuteWithoutShell(String mExecutable) {
        if (mExecutable == null || mExecutable.equals("")) {
            return -65536;
        }

        int exitValue = -65536;
        try {
            Process mProcess = Runtime.getRuntime().exec(new String[]{
                    "su", "-c", mExecutable
            });
            mProcess.waitFor();
            exitValue = mProcess.exitValue();
            mProcess.destroy();
            mProcess = null;
        } catch (IOException e) {
            Log.e("ViPER4", "ShellCommand IOException, msg = " + e.getMessage());
            return exitValue;
        } catch (InterruptedException e) {
            Log.e("ViPER4", "ShellCommand InterruptedException, msg = " + e.getMessage());
            return exitValue;
        }

        return exitValue;
    }

}
