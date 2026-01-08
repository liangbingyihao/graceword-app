package sdk.chat.demo;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

//import sdk.chat.contact.ContactBookModule;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.Device;
import sdk.chat.demo.bible.DynamicBibleDatabaseManager;
import sdk.chat.demo.robot.ChatSDKGW;
import sdk.chat.demo.robot.extensions.DateLocalizationUtil;
import sdk.chat.demo.robot.extensions.LanguageUtils;
import sdk.chat.demo.robot.extensions.FirebaseReport;
import sdk.chat.demo.robot.extensions.TinyLoggerManager;
import sdk.chat.demo.robot.handlers.AuthService;
import sdk.chat.demo.robot.handlers.CardApiService;
import sdk.chat.demo.robot.handlers.GWAuthenticationHandler;
import sdk.chat.demo.robot.handlers.LogUploader;
import sdk.chat.demo.robot.push.UpdateTokenWorker;
import sdk.guru.common.DisposableMap;
import sdk.guru.common.RX;

import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

//import com.bytedance.speech.speechengine.SpeechEngineGenerator;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.tinylog.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainApp extends Application implements Configuration.Provider, Application.ActivityLifecycleCallbacks {
    private static MainApp context;
    private boolean isInitialized = false;
    private Activity currentActivity;
    private ChatSDK chatSDK;
    public long startTimeStamp;
    public static String isNewUser = "0";
    private DynamicBibleDatabaseManager bibleDBManager;


    // 全局的 CompositeDisposable
    private static final CompositeDisposable appDisposables = new CompositeDisposable();

    // 模块级的 disposables
    private static final Map<Class<?>, CompositeDisposable> moduleDisposables = new HashMap<>();

    public static Context getContext() {
        return context;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    private void scheduleTokenUpdate() {
        // 创建每7天执行一次的定期工作请求
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                UpdateTokenWorker.class,
                7, // 重复间隔
                TimeUnit.DAYS)
                .addTag("FCM_TOKEN_UPDATE")
                .build();

        // 使用唯一工作名称确保只有一个实例运行
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "FCM_TOKEN_UPDATE_WORK",
                ExistingPeriodicWorkPolicy.KEEP, // 如果已有相同工作则保留
                workRequest);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startTimeStamp = System.currentTimeMillis();
        TinyLoggerManager.initialize(this);
        registerActivityLifecycleCallbacks(this);
        Log.i("MainApp", getPackageName());
        Logger.error("MainApp.onCreate");
        context = this;
        scheduleTokenUpdate();

//        SpeechEngineGenerator.PrepareEnvironment(getApplicationContext(), this);

        try {
            // Setup Chat SDK
            boolean drawerEnabled = !Device.honor();
            ChatSDKGW.quickStartWithEmail(this, drawerEnabled, "");
//            ContactBookModule.shared()

            chatSDK = ChatSDK.shared();
            addGlobalDisposable(AuthService.INSTANCE.authenticate(null)
                    .observeOn(RX.main())
                    .doFinally(AuthService.INSTANCE::ensureDatabase)
                    .subscribe(
                            () -> {
                                Logger.error("authenticate done");
                                isInitialized = true;
                            },
                            error -> { /* 错误处理 */
                                Logger.error(error, "authenticate error");
                                FirebaseReport.INSTANCE.reportExportEvent("app.init", "authenticate error", error);
                                isInitialized = false;
                            }
                    ));
        } catch (Exception e) {
            Logger.error(e, "MainApp.onCreate");
            FirebaseReport.INSTANCE.reportExportEvent("app.init", "init error", e);
        }
        setupEnhancedCrashReporting();
        LanguageUtils.INSTANCE.initAppLanguage(this);

        String installDay = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("install_day", "");
        String today = DateLocalizationUtil.INSTANCE.formatDayAgo(0);
        if (installDay.isEmpty()) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("install_day", today).apply();
        }
        isNewUser = today.equals(installDay) ? "1" : "0";

        bibleDBManager = DynamicBibleDatabaseManager.Companion.getInstance(this);
        bibleDBManager.initialize(this);
        CardApiService.INSTANCE.setLauncherStep(CardApiService.LauncherStep.INIT);

// 注册内存警告监听
        registerComponentCallbacks(new ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                handleMemoryPressure(level);
            }

            @Override
            public void onConfigurationChanged(@NonNull android.content.res.Configuration configuration) {

            }

            @Override
            public void onLowMemory() {
                clearAllDisposables();
            }
        });

        LogUploader.reportEvent(
                "app_launch", List.of(
                )
        );
//        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
    }

    public DynamicBibleDatabaseManager getBibleDBManager() {
        return bibleDBManager;
    }

    public static MainApp getInstance() {
        return context;
    }

    private void setupEnhancedCrashReporting() {
        Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                Logger.error(ex, "uncaughtException");
            } catch (Exception e) {

            }
            try {
                FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

                // 添加上下文信息
                crashlytics.setCustomKey("process", getApplicationInfo().processName);
                crashlytics.setCustomKey("last_activity",
                        currentActivity != null ? currentActivity.getClass().getSimpleName() : "none");
                crashlytics.setCustomKey("app_version",
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName);

                // 记录异常
                crashlytics.recordException(ex);
                crashlytics.sendUnsentReports();

            } catch (Exception e) {
                Log.e("CRASH_REPORT", "Error in crash handler", e);
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                } else {
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        currentActivity = null;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    /**
     * 添加全局 Disposable
     */
    public static void addGlobalDisposable(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            appDisposables.add(disposable);
        }
    }

    /**
     * 添加模块级 Disposable
     */
    public static <T> void addModuleDisposable(Class<T> moduleClass, Disposable disposable) {
        if (disposable == null || disposable.isDisposed()) {
            return;
        }

        synchronized (moduleDisposables) {
            CompositeDisposable cd = moduleDisposables.computeIfAbsent(moduleClass, k -> new CompositeDisposable());
            cd.add(disposable);
        }
    }

    /**
     * 清理指定模块的 Disposable
     */
    public static <T> void clearModuleDisposables(Class<T> moduleClass) {
        synchronized (moduleDisposables) {
            CompositeDisposable cd = moduleDisposables.remove(moduleClass);
            if (cd != null) {
                cd.clear();
            }
        }
    }
    /**
     * 处理内存压力
     */
    private void handleMemoryPressure(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // 应用进入后台，清理非必要的 Disposable
//            clearLowPriorityDisposables();
        }

        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            // 内存紧张，清理更多
//            clearMediumPriorityDisposables();
        }

        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            // 内存极度紧张，清理所有可清理的
            clearAllDisposables();
        }
    }


    private void clearAllDisposables() {
        appDisposables.clear();
        synchronized (moduleDisposables) {
            for (CompositeDisposable cd : moduleDisposables.values()) {
                cd.clear();
            }
            moduleDisposables.clear();
        }
    }

    @Override
    public void onTerminate() {
        // 应用终止时彻底清理
        if (!appDisposables.isDisposed()) {
            appDisposables.dispose();
        }
        super.onTerminate();
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
