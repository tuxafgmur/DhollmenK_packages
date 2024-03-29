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

import com.cyanogenmod.filemanager.commands.ParentDirExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;

import java.io.File;


/**
 * A class for returns the parent directory.
 */
public class ParentDirCommand extends Program implements ParentDirExecutable {

    private static final String TAG = "ParentDirCommand"; //$NON-NLS-1$

    private final String mSrc;
    private String mParentDir;

    /**
     * Constructor of <code>ParentDirCommand</code>.
     *
     * @param src The source file
     */
    public ParentDirCommand(String src) {
        super();
        this.mSrc = src;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResult() {
        return this.mParentDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException {

        File f = new File(this.mSrc);
        this.mParentDir = f.getParent();
    }

}
