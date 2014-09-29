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

package com.cyanogenmod.filemanager.console;

/**
 * An exception thrown when the file or directory is not found.
 */
public class NoSuchFileOrDirectory extends Exception {

    private static final long serialVersionUID = 8601894104043734066L;

    /**
     * Constructor of <code>NoSuchFileOrDirectory</code>.
     */
    public NoSuchFileOrDirectory() {
        super();
    }

    /**
     * Constructor of <code>NoSuchFileOrDirectory</code>.
     *
     * @param src The file or directory not found
     */
    public NoSuchFileOrDirectory(String src) {
        super(src);
    }

}
