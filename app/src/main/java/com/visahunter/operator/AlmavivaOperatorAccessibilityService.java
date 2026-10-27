package com.visahunter.operator;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class AlmavivaOperatorAccessibilityService extends AccessibilityService {
    private static final String TAG = "OperatorA11yService";
    private static AlmavivaOperatorAccessibilityService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long lastClickTime = 0;

    public static boolean isRunning() {
        return instance != null;
    }

    public static AlmavivaOperatorAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "AlmavivaOperatorAccessibilityService connected!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.i(TAG, "AlmavivaOperatorAccessibilityService destroyed!");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        CharSequence pkg = event.getPackageName();
        if (pkg != null && pkg.toString().equals("it.almaviva.visti")) {
            scanAndClickAlmavivaElements();
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Service interrupted");
    }

    public synchronized void scanAndClickAlmavivaElements() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 300) {
            return; // Throttle clicks to avoid multi-tap loops
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            // Target keywords for Almaviva confirmation / QR approval
            String[] targetKeywords = new String[]{
                    "مسح الاستجابة السريعة",
                    "امسح الاستجابة السريعة",
                    "Scan QR",
                    "Scansiona",
                    "Conferma",
                    "Confirm",
                    "تأكيد",
                    "سماح",
                    "Allow",
                    "Accetta",
                    "موافق",
                    "Sign In",
                    "Accedi",
                    "تسجيل الدخول"
            };

            for (String keyword : targetKeywords) {
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(keyword);
                if (nodes != null && !nodes.isEmpty()) {
                    for (AccessibilityNodeInfo node : nodes) {
                        if (performClickOnNodeOrParent(node)) {
                            lastClickTime = System.currentTimeMillis();
                            Log.i(TAG, "✅ Auto-clicked target element with keyword: " + keyword);
                            return;
                        }
                    }
                }
            }
        } finally {
            root.recycle();
        }
    }

    private boolean performClickOnNodeOrParent(AccessibilityNodeInfo node) {
        if (node == null) return false;

        AccessibilityNodeInfo current = node;
        for (int i = 0; i < 4; i++) {
            if (current == null) break;
            if (current.isClickable() && current.isVisibleToUser()) {
                boolean clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (clicked) return true;
            }
            current = current.getParent();
        }

        // Fallback: Gesture tap by bounding rect
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() > 0 && bounds.height() > 0) {
            dispatchTapGesture(bounds.centerX(), bounds.centerY());
            return true;
        }

        return false;
    }

    public void dispatchTapGesture(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
        dispatchGesture(builder.build(), null, null);
    }
}
