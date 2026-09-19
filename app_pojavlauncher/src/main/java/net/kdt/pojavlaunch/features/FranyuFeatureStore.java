package net.kdt.pojavlaunch.features;

import android.content.Context;
import android.content.SharedPreferences;

public final class FranyuFeatureStore {
    private static final String PREFS = "franyu_features";
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static boolean lowBattery(Context c) { return prefs(c).getBoolean("low_battery", true); }
    public static int lowBatteryThreshold(Context c) { return prefs(c).getInt("low_battery_threshold", 20); }
    public static boolean slowCharge(Context c) { return prefs(c).getBoolean("slow_charge", true); }
    public static boolean thermal(Context c) { return prefs(c).getBoolean("thermal", true); }
    public static int thermalThreshold(Context c) { return prefs(c).getInt("thermal_threshold", 45); }
    public static boolean diagnostics(Context c) { return prefs(c).getBoolean("diagnostics", true); }
    public static boolean modConflicts(Context c) { return prefs(c).getBoolean("mod_conflicts", true); }
    public static boolean storagePrecheck(Context c) { return prefs(c).getBoolean("storage_precheck", true); }
    public static boolean animatedBackground(Context c) { return prefs(c).getBoolean("animated_bg", false); }
    public static boolean chargeAndPlay(Context c) { return prefs(c).getBoolean("charge_play", true); }
    public static int cleanupDays(Context c) { return prefs(c).getInt("cleanup_days", 30); }
    public static boolean leakCheck(Context c) { return prefs(c).getBoolean("leak_check", true); }
    public static int scheduledHour(Context c) { return prefs(c).getInt("scheduled_hour", -1); }
    public static int accentColor(Context c) { return prefs(c).getInt("accent", 0); }
    public static int backgroundColor(Context c) { return prefs(c).getInt("background", 0); }
    public static void setBoolean(Context c, String key, boolean value) { prefs(c).edit().putBoolean(key, value).apply(); }
    public static void setInt(Context c, String key, int value) { prefs(c).edit().putInt(key, value).apply(); }
    public static void setColors(Context c, int accent, int background) { prefs(c).edit().putInt("accent", accent).putInt("background", background).apply(); }
    private FranyuFeatureStore() {}
}
