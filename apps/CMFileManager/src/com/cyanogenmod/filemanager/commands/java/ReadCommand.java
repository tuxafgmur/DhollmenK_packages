/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.commands.java;

import android.util.Log;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ReadExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * A class for read a file.
 */
public class ReadCommand extends Program implements ReadExecutable {

    private static final String TAG = "ReadCommand"; //$NON-NLS-1$

    private final String mFile;
    private final AsyncResultListener mAsyncResultListener;

    private boolean mCancelled;
    private boolean mEnded;
    private final Object mSync = new Object();

    /**
     * Constructor of <code>ExecCommand</code>.
     *
     * @param file The file to read
     * @param asyncResultListener The partial result listener
     */
    public ReadCommand(
            String file, AsyncResultListener asyncResultListener) {
        super();
        this.mFile = file;
        this.mAsyncResultListener = asyncResultListener;
        this.mCancelled = false;
        this.mEnded = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsynchronous() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException {
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncStart();
        }

        File f = new File(this.mFile);
        if (!f.exists()) {
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(new NoSuchFileOrDirectory(this.mFile));
            }
        }
        if (!f.isFile()) {
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(
                        new ExecutionException("path exists but it's not a file")); //$NON-NLS-1$
            }
        }

        // Read the file
        read(f);

        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncEnd(this.mCancelled);
        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncExitCode(0);
        }

    }

    /**
     * Method that read the file
     *
     * @param file The file to read
     */
    private void read(File file) {
        // Read the file
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file), getBufferSize());
            int read = 0;
            byte[] data = new byte[getBufferSize()];
            while ((read = bis.read(data, 0, getBufferSize())) != -1) {
                if (this.mAsyncResultListener != null) {
                    byte[] readData = new byte[read];
                    System.arraycopy(data, 0, readData, 0, read);
                    this.mAsyncResultListener.onPartialResult(readData);

                    // Check if the process was cancelled
                    try {
                        synchronized (this.mSync) {
                            if (this.mCancelled  || this.mEnded) {
                                this.mSync.notify();
                                break;
                            }
                        }
                    } catch (Exception e) {/**NON BLOCK**/}
                }
            }

        } catch (Exception e) {
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(new InsufficientPermissionsException());
            }

        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Throwable _throw) {/**NON BLOCK**/}
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        synchronized (this.mSync) {
            return this.mCancelled;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel() {
        try {
            synchronized (this.mSync) {
                this.mCancelled = true;
                this.mSync.wait(5000L);
            }
        } catch (Exception e) {/**NON BLOCK**/}
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean end() {
        try {
            synchronized (this.mSync) {
                this.mEnded = true;
                this.mSync.wait(5000L);
            }
        } catch (Exception e) {/**NON BLOCK**/}
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnEndListener(OnEndListener onEndListener) {
        //Ignore. Java console don't use this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        //Ignore. Java console don't use this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncResultListener getAsyncResultListener() {
        return this.mAsyncResultListener;
    }
}
