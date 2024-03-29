package com.butterfly.sdk;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.butterfly.sdk.utils.Utils;

import java.lang.ref.WeakReference;

public class BFContextProvider extends ContentProvider {
    // Nullable
    public static Context getApplicationContext() {
        WeakReference<Context> contextWeakReference = Utils.Companion.getApplicationContextWeakReference();
        if (contextWeakReference == null) return null;

        return contextWeakReference.get();
    }

    @Override
    public boolean onCreate() {
        Utils.Companion.saveContext(getContext().getApplicationContext());

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
