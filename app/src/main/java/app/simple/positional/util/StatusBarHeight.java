package app.simple.positional.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import app.simple.positional.R;

public class StatusBarHeight {
    public static int getStatusBarHeight(Window window) {
        Rect rectangle = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        return rectangle.top - window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
    }

    public static int getStatusBarHeight(Resources resources) {
        int result = 0;
        @SuppressLint({"DiscouragedApi", "InternalInsetResource"}) int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavigationBarHeight(Resources resources) {
        @SuppressLint({"InternalInsetResource", "DiscouragedApi"}) int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            if (isEdgeToEdgeEnabled(resources) == 2) {
                return 0;
            }
            else {
                return resources.getDimensionPixelSize(resourceId);
            }
        }
        return 0;
    }

    @Deprecated
    public static int getToolBarHeight(Context context) {
        int i = 0;
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            i = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }

        return i;
    }

    public static int isEdgeToEdgeEnabled(Resources resources) {
        int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");
        if (resourceId > 0) {
            return resources.getInteger(resourceId);
        }
        return 0;
    }

    public static boolean isLandscape(Context context) {
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            return false;
        }

        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
    }
}
