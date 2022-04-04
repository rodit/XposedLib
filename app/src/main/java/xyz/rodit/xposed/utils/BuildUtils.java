package xyz.rodit.xposed.utils;

import android.content.Context;
import android.content.pm.PackageInfo;

import de.robv.android.xposed.XposedBridge;

public class BuildUtils {

    public static int getBuildVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            XposedBridge.log("Error getting build version.");
            XposedBridge.log(e);
            return 0;
        }
    }
}
