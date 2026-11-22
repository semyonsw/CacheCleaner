package com.semyon.cachecleaner;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CacheAccessibilityService extends AccessibilityService {
    private static CacheAccessibilityService instance;
    private static final ArrayDeque<String> packages = new ArrayDeque<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private boolean actionSent;
    private String currentPackage;
    private int processed;
    private int skipped;

    public static void start(List<String> packageNames) {
        packages.clear();
        packages.addAll(packageNames);
        if (instance != null) {
            instance.begin();
        }
    }

    public static void stop() {
        if (instance != null) {
            instance.finish();
        }
    }

    public static boolean isRunning() {
        return instance != null && instance.running;
    }

    public static int getProcessedCount() {
        return instance == null ? 0 : instance.processed;
    }

    public static int getSkippedCount() {
        return instance == null ? 0 : instance.skipped;
    }

    @Override
    public void onServiceConnected() {
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new AccessibilityServiceInfo();
        }
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 200;
        setServiceInfo(info);
        if (!packages.isEmpty()) {
            begin();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        finish();
        instance = null;
        return false;
    }

    private void begin() {
        if (running) {
            return;
        }
        running = true;
        processed = 0;
        skipped = 0;
        openNext();
    }

    private void finish() {
        running = false;
        actionSent = false;
        currentPackage = null;
        packages.clear();
        handler.removeCallbacksAndMessages(null);
    }

    private void openNext() {
        if (!running) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
        currentPackage = packages.pollFirst();
        actionSent = false;
        if (currentPackage == null) {
            running = false;
            return;
        }

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + currentPackage));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                skipped++;
                openNext();
            }
        }, 6500);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!running || currentPackage == null) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        try {
            if (tryClearCacheButton(root)) {
                actionSent = true;
                processed++;
                root.recycle();
                scheduleNext(900);
                return;
            }

            if (actionSent) {
                root.recycle();
                scheduleNext(350);
                return;
            }

            if (tryStorageButton(root)) {
                root.recycle();
                return;
            }
        } catch (Exception ignored) {
            root.recycle();
        }
    }

    private boolean tryStorageButton(AccessibilityNodeInfo root) {
        if (actionSent) {
            return false;
        }
        AccessibilityNodeInfo node = findNode(root, new String[]{
                "storage", "data usage", "memoria", "storage usage",
                "xe0","szabad tárhely", "保管", "存储", "저장공간", "хранилище"
        });
        if (node == null) {
            return false;
        }
        boolean clicked = node.isClickable() ? node.performAction(AccessibilityNodeInfo.ACTION_CLICK) :
                clickClosestAncestor(node);
        node.recycle();
        return clicked;
    }

    private boolean tryClearCacheButton(AccessibilityNodeInfo root) {
        if (actionSent) {
            return false;
        }
        AccessibilityNodeInfo node = findNode(root, new String[]{
                "clear cache", "delete cache", "clearcached data", "clear cached data",
                "clearcache", "clear cache data", "cached data", "cached data",
                "캐시 삭제", "缓存", "キャッシュを削除", "缓存清除",
                "缓存数据", "kcache löschen", "cache leeren"
        });
        if (node == null) {
            return false;
        }
        String label = normalized(node.getText());
        String description = normalized(node.getContentDescription());
        String combined = (label + " " + description).trim();
        if (combined.contains("clear data")
                || combined.contains("delete data")
                || combined.contains("erase data")
                || combined.contains("data will be permanently")
                || combined.contains("remove app data")
                || combined.contains("all data")
                || combined.contains("gegevens wissen")
                || combined.contains("앱 데이터 삭제")
                || combined.contains("删除数据")
                || combined.contains("データを削除")) {
            node.recycle();
            return false;
        }
        if (node.isClickable()) {
            boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            node.recycle();
            return clicked;
        }
        boolean clicked = clickClosestAncestor(node);
        node.recycle();
        return clicked;
    }

    private boolean clickClosestAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            if (current.isClickable()
                    && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            AccessibilityNodeInfo parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return false;
    }

    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo root, String[] phrases) {
        if (root == null) {
            return null;
        }

        if (matches(root, phrases) && root.isEnabled()) {
            return AccessibilityNodeInfo.obtain(root);
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child == null) {
                continue;
            }
            AccessibilityNodeInfo result = findNode(child, phrases);
            child.recycle();
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean matches(AccessibilityNodeInfo node, String[] phrases) {
        String text = normalized(node.getText());
        String description = normalized(node.getContentDescription());
        String viewId = normalized(node.getViewIdResourceName());

        for (String phrase : phrases) {
            String normalizedPhrase = normalized(phrase);
            if (!normalizedPhrase.isEmpty()
                    && (text.contains(normalizedPhrase)
                    || description.contains(normalizedPhrase)
                    || viewId.contains(normalizedPhrase))) {
                return true;
            }
        }
        return false;
    }

    private String normalized(CharSequence value) {
        return value == null ? "" : value.toString().toLowerCase(java.util.Locale.ROOT).trim();
    }

    private void scheduleNext(long delay) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (running) {
                    openNext();
                }
            }
        }, delay);
    }

    @Override
    public void onInterrupt() {
        finish();
    }
}
