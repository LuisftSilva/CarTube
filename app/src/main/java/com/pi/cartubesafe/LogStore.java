package com.pi.cartubesafe;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LogStore {
    public static final int REQUEST_LINK_DRIVE = 401;
    private static final String PREFS = "diagnostics";
    private static final String PREF_DRIVE_URI = "drive_uri";
    private static final long MAX_LOG_BYTES = 1024L * 1024L;

    private static Context appContext;
    private static File logFile;
    private static Thread.UncaughtExceptionHandler previousHandler;

    private LogStore() {}

    public static synchronized void init(Context context) {
        if (appContext != null) return;
        appContext = context.getApplicationContext();
        logFile = new File(appContext.getFilesDir(), "CarTubeSafe-diagnostics.log");
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            e("CRASH", "Uncaught exception in " + thread.getName(), throwable);
            syncDriveBestEffort();
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            }
        });
    }

    public static void i(String tag, String message) {
        append("I", tag, message, null);
    }

    public static void w(String tag, String message) {
        append("W", tag, message, null);
    }

    public static void e(String tag, String message, Throwable error) {
        append("E", tag, message, error);
    }

    private static synchronized void append(String level, String tag, String message, Throwable error) {
        if (appContext == null || logFile == null) return;
        try {
            rotateIfNeeded();
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(ts + " [" + level + "] " + tag + ": " + message + "\n");
                if (error != null) {
                    writer.write(android.util.Log.getStackTraceString(error));
                    writer.write("\n");
                }
                writer.flush();
            }
        } catch (Exception ignored) {
        }
    }

    private static void rotateIfNeeded() {
        if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
            File old = new File(logFile.getParentFile(), "CarTubeSafe-diagnostics.previous.log");
            if (old.exists()) old.delete();
            logFile.renameTo(old);
            logFile = new File(appContext.getFilesDir(), "CarTubeSafe-diagnostics.log");
        }
    }

    public static void requestDriveLink(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "CarTubeSafe-diagnostics.log");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_LINK_DRIVE);
    }

    public static boolean handleDriveLinkResult(Context context, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_LINK_DRIVE || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return false;
        }
        Uri uri = data.getData();
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            context.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREF_DRIVE_URI, uri.toString()).apply();
        i("LogStore", "Drive diagnostics destination linked: " + uri);
        syncDriveBestEffort();
        return true;
    }

    public static boolean hasDriveLink() {
        if (appContext == null) return false;
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(PREF_DRIVE_URI, null) != null;
    }

    public static synchronized boolean syncDriveBestEffort() {
        if (appContext == null || logFile == null || !logFile.exists()) return false;
        String uriString = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(PREF_DRIVE_URI, null);
        if (uriString == null) return false;
        try {
            Uri uri = Uri.parse(uriString);
            ContentResolver resolver = appContext.getContentResolver();
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }
            try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
                if (out == null) return false;
                out.write(content.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            return true;
        } catch (Exception error) {
            append("W", "LogStore", "Drive sync failed: " + error.getClass().getSimpleName() + ": " + error.getMessage(), null);
            return false;
        }
    }
}
