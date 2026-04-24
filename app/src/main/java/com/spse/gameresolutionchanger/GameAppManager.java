package com.spse.gameresolutionchanger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameAppManager {

    public static List<GameApp> getGameApps(MainActivity context, boolean onlyAddGames) {
        PackageManager pm = context.getPackageManager();
        List<GameApp> gameAppList = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> launcherApps = pm.queryIntentActivities(launcherIntent, 0);
        for (ResolveInfo resolveInfo : launcherApps) {
            if (resolveInfo == null || resolveInfo.activityInfo == null || resolveInfo.activityInfo.applicationInfo == null) {
                continue;
            }

            ApplicationInfo appInfo = resolveInfo.activityInfo.applicationInfo;
            String packageName = appInfo.packageName;
            if (packageName == null || !seenPackages.add(packageName)) {
                continue;
            }

            if (!isValidPackage(context, appInfo, onlyAddGames)) {
                continue;
            }

            String appName = appInfo.loadLabel(pm).toString();
            WrappedDrawable wrappedIcon = new WrappedDrawable(appInfo.loadIcon(pm), 0, 0, 160, 160);
            gameAppList.add(new GameApp(appName, wrappedIcon, packageName));
        }
        return gameAppList;
    }

    public static GameApp getGameApp(Context context, String packageName) {
        if (packageName == null || packageName.equals("")) return null;
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo app = getApplicationInfoCompat(pm, packageName);
            WrappedDrawable wrappedIcon = new WrappedDrawable(pm.getApplicationIcon(app), 0, 0, 180, 180);
            String name = pm.getApplicationLabel(app).toString();
            return new GameApp(name, wrappedIcon, packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static ApplicationInfo getApplicationInfoCompat(PackageManager pm, String packageName)
            throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0));
        }
        return pm.getApplicationInfo(packageName, 0);
    }

    private static boolean isSystemPackage(ApplicationInfo appInfo) {
        int flags = appInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    @SuppressLint("NewApi")
    private static boolean isGamePackage(ApplicationInfo appInfo, boolean onlyAddGames) {
        if (!onlyAddGames) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return true;
        }
        return (appInfo.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
    }

    private static boolean isValidPackage(MainActivity context, ApplicationInfo appInfo, boolean onlyAddGames) {
        String packageName = appInfo.packageName;
        if (!isGamePackage(appInfo, onlyAddGames)
                || isSystemPackage(appInfo)
                || packageName.equals(context.getApplicationContext().getPackageName())) {
            return false;
        }

        for (int i = 1; i <= 6; i++) {
            GameApp recent = context.settingsManager.getRecentGameApp(i);
            if (recent != null && recent.getPackageName().equals(packageName)) {
                return false;
            }
        }
        return true;
    }

    private static void murderApps(MainActivity context) {
        ArrayList<String> murdererList = new ArrayList<>();
        for (GameApp app : getGameApps(context, false)) {
            String packageName = app.getPackageName();
            if (!packageName.equals(context.getApplication().getPackageName())
                    && !packageName.equals("com.topjohnwu.magisk")
                    && !packageName.equals("eu.chainfire.supersu")) {
                murdererList.add("am force-stop " + packageName);
            }
        }
        ExecuteADBCommands.execute(murdererList, true);
    }

    private static void activateLMK(MainActivity context) {
        String dataDir = context.getApplicationInfo().dataDir;
        ArrayList<String> commands = new ArrayList<>();
        commands.add("if [ -e /sys/module/lowmemorykiller/parameters/minfree ]; then "
                + "cat /sys/module/lowmemorykiller/parameters/minfree > " + dataDir + "/lastLMKProfile.backup; "
                + "echo 2560,5120,11520,25600,35840,358400 > /sys/module/lowmemorykiller/parameters/minfree; "
                + "fi");
        ExecuteADBCommands.execute(commands, true);
    }

    public static void restoreOriginalLMK(MainActivity context) {
        String dataDir = context.getApplicationInfo().dataDir;
        ExecuteADBCommands.execute("if [ -e " + dataDir + "/lastLMKProfile.backup ] "
                + "&& [ -e /sys/module/lowmemorykiller/parameters/minfree ]; then "
                + "cat " + dataDir + "/lastLMKProfile.backup > /sys/module/lowmemorykiller/parameters/minfree; fi", true);
        new File(dataDir + "/lastLMKProfile.backup").delete();
    }

    public static void launchGameApp(MainActivity context, String packageName) {
        SettingsManager st = context.settingsManager;
        int resolutionScale = context.resolutionSeekBar.getProgress();

        st.setLastResolutionScale(resolutionScale);

        if (!st.keepStockDPI()) {
            ExecuteADBCommands.execute("echo '' > " + context.getApplicationInfo().dataDir + "/tmp", st.isRoot());
        }

        boolean changed = st.setScreenDimension(
                (int) (Math.ceil(context.coefficients[1] * resolutionScale) + st.getOriginalHeight()),
                (int) (Math.ceil(context.coefficients[0] * resolutionScale) + st.getOriginalWidth())
        );

        if (!changed) {
            Toast.makeText(context, "Cannot apply resolution. Check root/ADB permission.", Toast.LENGTH_LONG).show();
        }

        if (st.isMurderer()) {
            murderApps(context);
        }

        if (st.isLMKActivated()) {
            activateLMK(context);
        }

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            context.finish();
        } else {
            Toast.makeText(context, "Cannot launch selected app.", Toast.LENGTH_LONG).show();
        }
    }
}
