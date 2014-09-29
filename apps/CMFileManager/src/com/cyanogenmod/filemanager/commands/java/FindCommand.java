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
import com.cyanogenmod.filemanager.commands.FindExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.SearchHelper;

import java.io.File;
import java.util.Arrays;

/**
 * A class for search files.
 */
public class FindCommand extends Program implements FindExecutable {

    private static final String TAG = "FindCommand"; //$NON-NLS-1$

    private final String mDirectory;
    private final String[] mQueryRegExp;
    private final AsyncResultListener mAsyncResultListener;

    private boolean mCancelled;
    private boolean mEnded;
    private final Object mSync = new Object();

    /**
     * Constructor of <code>FindCommand</code>.
     *
     * @param directory The absolute directory where start the search
     * @param query The terms to be searched
     * @param asyncResultListener The partial result listener
     */
    public FindCommand(String directory, Query query, AsyncResultListener asyncResultListener) {
        super();
        this.mDirectory = directory;
        this.mQueryRegExp = createRegexp(directory, query);
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
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Finding in %s the query %s", //$NON-NLS-1$
                            this.mDirectory, Arrays.toString(this.mQueryRegExp)));
        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncStart();
        }

        File f = new File(this.mDirectory);
        if (!f.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(new NoSuchFileOrDirectory(this.mDirectory));
            }
        }
        if (!f.isDirectory()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(
                        new ExecutionException("path exists but it's not a folder")); //$NON-NLS-1$
            }
        }

        // Find the data
        findRecursive(f);

        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncEnd(this.mCancelled);
        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncExitCode(0);
        }

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
        }
    }

    /**
     * Method that search files recursively
     *
     * @param folder The folder where to start the search
     */
    private void findRecursive(File folder) {
        // Obtains the files and folders of the folders
        File[] files = folder.listFiles();
        if (files != null) {
            int cc = files.length;
            for (int i = 0; i < cc; i++) {
                if (files[i].isDirectory()) {
                    findRecursive(files[i]);
                }

                // Check if the file or folder matches the regexp
                try {
                    int ccc = this.mQueryRegExp.length;
                    for (int j = 0; j < ccc; j++) {
                        if (files[i].getName().matches(this.mQueryRegExp[j])) {
                            FileSystemObject fso =
                                    FileHelper.createFileSystemObject(files[i]);
                            if (fso != null) {
                                if (isTrace()) {
                                    Log.v(TAG, String.valueOf(fso));
                                }
                                if (this.mAsyncResultListener != null) {
                                    this.mAsyncResultListener.onPartialResult(fso);
                                }
                            }
                        }
                    }
                } catch (Exception e) {/**NON-BLOCK**/}

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

    /**
     * Method that create the regexp of this command, using the directory and
     * arguments and creating the regular expressions of the search.
     *
     * @param directory The directory where to search
     * @param query The query make for user
     * @return String[] The regexp for filtering files
     */
    private static String[] createRegexp(String directory, Query query) {
        String[] args = new String[query.getSlotsCount()];
        int cc = query.getSlotsCount();
        for (int i = 0; i < cc; i++) {
            args[i] = SearchHelper.toIgnoreCaseRegExp(query.getSlot(i), true);
        }
        return args;
    }
}
