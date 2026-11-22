package com.semyon.cachecleaner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;

public class MainActivity extends Activity {
    private TextView cacheSizeText;
    private TextView statusText;
    private ProgressBar progressBar;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cacheSizeText = findViewById(R.id.cacheSizeText);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);
        clearButton = findViewById(R.id.clearButton);

        updateCacheSize();
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearOwnCache();
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
        long size = getOwnCacheSize();
        cacheSizeText.setText(formatBytes(size));
        statusText.setText(size == 0
                ? "No cache found for Cache Cleaner"
                : "Ready to remove Cache Cleaner cache");
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
        if (bytes < 1024 * 1024) {
            return new DecimalFormat("#.#").format(bytes / 1024d) + " KB";
        }
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
