package com.semyon.cachecleaner;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private TextView cacheSizeText;
    private TextView statusText;
    private ProgressBar progressBar;
    private Button clearButton;
    private CheckBox includeSystemCheck;
    private boolean cleaning;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cacheSizeText = findViewById(R.id.cacheSizeText);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);
        clearButton = findViewById(R.id.clearButton);
        includeSystemCheck = findViewById(R.id.includeSystemCheck);

        updateCacheSize();

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cleaning) {
                    stopCleaning();
                } else {
                    startCleaning();
                }
            }
        });

        findViewById(R.id.storageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openStorageSettings();
            }
        });

        findViewById(R.id.appsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAppManager();
            }
        });
    }

    private void updateCacheSize() {
        int count = getInstalledPackages().size();
        cacheSizeText.setText(String.valueOf(count));
        statusText.setText("Installed apps ready");
    }

    private long getOwnCacheSize() {
        long size = directorySize(getCacheDir());
        size += directorySize(getCodeCacheDir());
        File[] externalDirs = getExternalCacheDirs();
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                size += directorySize(dir);
            }
        }
        return size;
    }

    private void startCleaning() {
        if (!isAccessibilityEnabled()) {
            statusText.setText("Enable Cache Cleaner in Accessibility settings");
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "Turn on Cache Cleaner, then return and tap the button again",
                    Toast.LENGTH_LONG).show();
            return;
        }

        List<String> packageNames = getInstalledPackages();
        if (packageNames.isEmpty()) {
            statusText.setText("No apps available");
            return;
        }

        cleaning = true;
        progressBar.setVisibility(View.VISIBLE);
        clearButton.setText("STOP CLEANING");
        statusText.setText("Preparing " + packageNames.size() + " apps...");
        CacheAccessibilityService.start(packageNames);
        handler.post(progressRunnable);
    }

    private void stopCleaning() {
        CacheAccessibilityService.stop();
        cleaning = false;
        handler.removeCallbacks(progressRunnable);
        progressBar.setVisibility(View.GONE);
        clearButton.setText("CLEAR ALL APP CACHES");
        statusText.setText("Cleaning stopped");
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            int processed = CacheAccessibilityService.getProcessedCount();
            int skipped = CacheAccessibilityService.getSkippedCount();
            if (cleaning && CacheAccessibilityService.isRunning()) {
                statusText.setText("Cleared: " + processed + "  Skipped: " + skipped);
                handler.postDelayed(this, 500);
            } else if (cleaning) {
                cleaning = false;
                progressBar.setVisibility(View.GONE);
                clearButton.setText("CLEAR ALL APP CACHES");
                statusText.setText("Finished. Cleared: " + processed + "  Skipped: " + skipped);
            }
        }
    };

    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        String expected = getPackageName() + "/.CacheAccessibilityService";
        for (String service : enabled.split(":")) {
            if (service.equals(expected)
                    || service.endsWith("/com.semyon.cachecleaner.CacheAccessibilityService")) {
                return true;
            }
        }
        return false;
    }

    private List<String> getInstalledPackages() {
        PackageManager manager = getPackageManager();
        List<ApplicationInfo> apps = manager.getInstalledApplications(
                PackageManager.GET_META_DATA);
        List<String> names = new ArrayList<>();
        boolean includeSystem = includeSystemCheck.isChecked();

        for (ApplicationInfo app : apps) {
            if (app.packageName.equals(getPackageName())) {
                continue;
            }
            boolean isSystem = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (!includeSystem && isSystem) {
                continue;
            }
            names.add(app.packageName);
        }
        Collections.sort(names);
        return names;
    }

    private void clearOwnCache() {
        clearButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Cleaning...");

        long before = getOwnCacheSize();
        clearDirectory(getCacheDir());
        clearDirectory(getCodeCacheDir());
        File[] externalDirs = getExternalCacheDirs();
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                clearDirectory(dir);
            }
        }

        progressBar.setVisibility(View.GONE);
        clearButton.setEnabled(true);
        updateCacheSize();
        Toast.makeText(this, "Freed " + formatBytes(before), Toast.LENGTH_LONG).show();
    }

    private void clearDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                clearDirectory(file);
            }
            file.delete();
        }
    }

    private long directorySize(File directory) {
        if (directory == null || !directory.exists()) {
            return 0;
        }
        long size = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        for (File file : files) {
            size += file.isDirectory() ? directorySize(file) : file.length();
        }
        return size;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return new DecimalFormat("#.#").format(bytes / 1024d) + " KB";
        if (bytes < 1024 * 1024 * 1024) {
            return new DecimalFormat("#.#").format(bytes / 1048576d) + " MB";
        }
        return new DecimalFormat("#.#").format(bytes / 1073741824d) + " GB";
    }

    private void openStorageSettings() {
        Intent intent = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            openAppManager();
        }
    }

    private void openAppManager() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        }
    }
}
