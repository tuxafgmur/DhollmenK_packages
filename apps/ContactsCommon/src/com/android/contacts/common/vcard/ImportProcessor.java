/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.contacts.common.vcard;

import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardInterpreter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.exception.VCardVersionException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for processing one import request from a user. Dropped after importing requested Uri(s).
 * {@link VCardService} will create another object when there is another import request.
 */
public class ImportProcessor extends ProcessorBase implements VCardEntryHandler {
    private static final String LOG_TAG = "VCardImport";
    private static final boolean DEBUG = false;

    private final VCardService mService;
    private final ContentResolver mResolver;
    private final ImportRequest mImportRequest;
    private final int mJobId;
    private final VCardImportExportListener mListener;

    // TODO: remove and show appropriate message instead.
    private final List<Uri> mFailedUris = new ArrayList<Uri>();

    private VCardParser mVCardParser;

    private volatile boolean mCanceled;
    private volatile boolean mDone;

    private int mCurrentCount = 0;
    private int mTotalCount = 0;

    public ImportProcessor(final VCardService service, final VCardImportExportListener listener,
            final ImportRequest request, final int jobId) {
        mService = service;
        mResolver = mService.getContentResolver();
        mListener = listener;

        mImportRequest = request;
        mJobId = jobId;
    }

    @Override
    public void onStart() {
        // do nothing
    }

    @Override
    public void onEnd() {
        // do nothing
    }

    @Override
    public void onEntryCreated(VCardEntry entry) {
        mCurrentCount++;
        if (mListener != null) {
            mListener.onImportParsed(mImportRequest, mJobId, entry, mCurrentCount, mTotalCount);
        }
    }

    @Override
    public final int getType() {
        return VCardService.TYPE_IMPORT;
    }

    @Override
    public void run() {
        // ExecutorService ignores RuntimeException, so we need to show it here.
        try {
            runInternal();

            if (isCancelled() && mListener != null) {
                mListener.onImportCanceled(mImportRequest, mJobId);
            }
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "OutOfMemoryError thrown during import", e);
            throw e;
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "RuntimeException thrown during import", e);
            throw e;
        } finally {
            synchronized (this) {
                mDone = true;
            }
        }
    }

    private void runInternal() {
        final ImportRequest request = mImportRequest;
        if (isCancelled()) {
            return;
        }
        final int[] possibleVCardVersions;
        if (request.vcardVersion == ImportVCardActivity.VCARD_VERSION_AUTO_DETECT) {
            /**
             * Note: this code assumes that a given Uri is able to be opened more than once,
             * which may not be true in certain conditions.
             */
            possibleVCardVersions = new int[] {
                    ImportVCardActivity.VCARD_VERSION_V21,
                    ImportVCardActivity.VCARD_VERSION_V30
            };
        } else {
            possibleVCardVersions = new int[] {
                    request.vcardVersion
            };
        }

        final Uri uri = request.uri;
        final Account account = request.account;
        final int estimatedVCardType = request.estimatedVCardType;
        final String estimatedCharset = request.estimatedCharset;
        final int entryCount = request.entryCount;
        mTotalCount += entryCount;

        final VCardEntryConstructor constructor =
                new VCardEntryConstructor(estimatedVCardType, account, estimatedCharset);
        final VCardEntryCommitter committer = new VCardEntryCommitter(mResolver);
        constructor.addEntryHandler(committer);
        constructor.addEntryHandler(this);

        InputStream is = null;
        boolean successful = false;
        try {
            if (uri != null) {
                is = mResolver.openInputStream(uri);
            } else if (request.data != null){
                is = new ByteArrayInputStream(request.data);
            }

            if (is != null) {
                successful = readOneVCard(is, estimatedVCardType, estimatedCharset, constructor,
                        possibleVCardVersions);
            }
        } catch (IOException e) {
            successful = false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        mService.handleFinishImportNotification(mJobId, successful);

        if (successful) {
            // TODO: successful becomes true even when cancelled. Should return more appropriate
            // value
            if (isCancelled()) {
                // Cancel notification will be done outside this method.
            } else {
                List<Uri> uris = committer.getCreatedUris();
                if (mListener != null) {
                    if (uris != null && uris.size() > 0) {
                        // TODO: construct intent showing a list of imported contact list.
                        mListener.onImportFinished(mImportRequest, mJobId, uris.get(0));
                    } else {
                        // Not critical, but suspicious.
                        mListener.onImportFinished(mImportRequest, mJobId, null);
                    }
                }
            }
        } else {
            mFailedUris.add(uri);
        }
    }

    private boolean readOneVCard(InputStream is, int vcardType, String charset,
            final VCardInterpreter interpreter,
            final int[] possibleVCardVersions) {
        boolean successful = false;
        final int length = possibleVCardVersions.length;
        for (int i = 0; i < length; i++) {
            final int vcardVersion = possibleVCardVersions[i];
            try {
                if (i > 0 && (interpreter instanceof VCardEntryConstructor)) {
                    // Let the object clean up internal temporary objects,
                    ((VCardEntryConstructor) interpreter).clear();
                }

                // We need synchronized block here,
                // since we need to handle mCanceled and mVCardParser at once.
                // In the worst case, a user may call cancel() just before creating
                // mVCardParser.
                synchronized (this) {
                    mVCardParser = (vcardVersion == ImportVCardActivity.VCARD_VERSION_V30 ?
                            new VCardParser_V30(vcardType) :
                                new VCardParser_V21(vcardType));
                    if (isCancelled()) {
                        mVCardParser.cancel();
                    }
                }
                mVCardParser.parse(is, interpreter);

                successful = true;
                break;
            } catch (IOException e) {
            } catch (VCardNestedException e) {
            } catch (VCardNotSupportedException e) {
            } catch (VCardVersionException e) {
            } catch (VCardException e) {
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        return successful;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (DEBUG) Log.d(LOG_TAG, "ImportProcessor received cancel request");
        if (mDone || mCanceled) {
            return false;
        }
        mCanceled = true;
        synchronized (this) {
            if (mVCardParser != null) {
                mVCardParser.cancel();
            }
        }
        return true;
    }

    @Override
    public synchronized boolean isCancelled() {
        return mCanceled;
    }


    @Override
    public synchronized boolean isDone() {
        return mDone;
    }
}
