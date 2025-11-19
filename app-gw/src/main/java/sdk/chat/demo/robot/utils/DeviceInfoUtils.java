package sdk.chat.demo.robot.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.robot.api.model.KeyValuePair;
import sdk.chat.demo.robot.extensions.LanguageUtils;

public class DeviceInfoUtils {

    public static String getRegion(Context context) {
        try {
            // 1. 尝试从SIM卡获取
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String simCountry = tm.getSimCountryIso();
                if (simCountry != null && !simCountry.isEmpty()) {
                    return simCountry.toUpperCase();
                }

                // 2. 尝试从网络获取
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && !networkCountry.isEmpty()) {
                    return networkCountry.toUpperCase();
                }
            }

            // 3. 使用Locale获取
            Locale currentLocale = getCurrentLocale(context);
            return currentLocale.getCountry();

        } catch (Exception e) {
            // 4. 回退到默认
            return Locale.getDefault().getCountry();
        }
    }

    public static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getBundleId(Context context) {
        return context.getPackageName();
    }

    public static String getUserId(Context context) {
//        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
//        String userId = prefs.getString("user_id", null);
//        if (userId == null) {
//            userId = UUID.randomUUID().toString();
//            prefs.edit().putString("user_id", userId).apply();
//        }
        String uid = "";
        try {
            uid = ChatSDK.currentUserID();
        } catch (Exception ignored) {

        }
        return uid == null ? "" : uid;
    }

    public static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    public static String getDeviceUuid(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        String hardwareInfo = Build.BOARD + Build.BRAND + Build.DEVICE +
                Build.DISPLAY + Build.HOST + Build.MANUFACTURER +
                Build.MODEL + Build.PRODUCT + Build.TAGS +
                Build.TYPE + Build.USER;

        try {
            UUID uuid = UUID.nameUUIDFromBytes(hardwareInfo.getBytes("utf8"));
            return uuid.toString();
        } catch (UnsupportedEncodingException e) {
            return androidId;
        }
    }

    public static String getAllDeviceInfo(Context context) {
        return String.format(Locale.US,
                "Region: %s\n" +
                        "Model: %s\n" +
                        "OS Version: %s\n" +
                        "Bundle ID: %s\n" +
                        "User ID: %s\n" +
                        "App Version: %s\n" +
                        "Device UUID: %s",
                getRegion(context),
                getDeviceModel(),
                getOSVersion(),
                getBundleId(context),
                getUserId(context),
                getAppVersion(context),
                getDeviceUuid(context)
        );
    }

    private static List<KeyValuePair> deviceInfos = null;

    public static List<KeyValuePair> getAllDeviceInfoKvs(Context context) {
        if (deviceInfos == null) {
            String appLang = LanguageUtils.INSTANCE.getAppLanguage(context, false);
            appLang = appLang == null ? "" : appLang;
            deviceInfos = List.of(
                    new KeyValuePair("newUser", MainApp.isNewUser),
                    new KeyValuePair("region", getRegion(context)),
                    new KeyValuePair("model", getDeviceModel()),
                    new KeyValuePair("osVersion", getOSVersion()),
                    new KeyValuePair("bundleId", getBundleId(context)),
                    new KeyValuePair("appVersion", getAppVersion(context)),
                    new KeyValuePair("deciceUuid", getDeviceUuid(context)),
                    new KeyValuePair("userId", getUserId(context)),
                    new KeyValuePair("app_language", appLang),
                    new KeyValuePair("system_language", LanguageUtils.INSTANCE.getSystemLanguage())
            );
        }
        return deviceInfos;
    }
}