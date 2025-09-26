package sdk.guru.common;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.internal.schedulers.SingleScheduler;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class RX {

    protected static ExecutorService firebaseExecutorService;

    // Used for network tasks where the order matters i.e. login then send message
    protected static final Scheduler ioSingle = RxJavaPlugins.initSingleScheduler(new SingleIOTask());


    public static final class IOSingleHolder {
        static final Scheduler DEFAULT = new SingleScheduler();
    }

    public static final class SingleIOTask implements Callable<Scheduler> {
        @Override
        public Scheduler call() throws Exception {
            return IOSingleHolder.DEFAULT;
        }
    }

    @NonNull
    public static Scheduler ioSingle() {
        return RxJavaPlugins.onSingleScheduler(ioSingle);
    }


    public static Scheduler single() {
        return Schedulers.single();
    }

    public static Scheduler io() {
        return Schedulers.io();
    }


    public static Scheduler computation() {
        return Schedulers.computation();
    }

    public static Scheduler main() {
        return AndroidSchedulers.mainThread();
    }

    /**
     * Quick tasks that will finish fast
     * @return scheduler
     */
    public static Scheduler quick() {
        return Schedulers.computation();
    }

    /**
     * Database operations
     * @return scheduler
     */
    public static Scheduler db() {
        return Schedulers.computation();
    }

    /**
     * For longer listeners
     * @return scheduler
     */
    public static Scheduler pool() {
        if (firebaseExecutorService == null) {
            firebaseExecutorService = Executors.newFixedThreadPool(10);
        }
        return Schedulers.from(firebaseExecutorService);
    }

    public static Scheduler newThread() {
        return Schedulers.newThread();
    }

    public static void onMain(Runnable runnable) {
        main().scheduleDirect(runnable);
    }

    public static void onBackground(Runnable runnable) {
        computation().scheduleDirect(runnable);
    }

    // Note, this an fail silently if there is an error
    public static Completable run(final Runnable onBackground, final Runnable onMain) {
        return Completable.create(emitter -> {
            onBackground.run();
            emitter.onComplete();
        }).subscribeOn(computation()).observeOn(main()).doOnComplete(onMain::run);
    }

    public static Completable runSingle(final Runnable onBackground, final Runnable onMain) {
        return Completable.create(emitter -> {
            onBackground.run();
            emitter.onComplete();
        }).subscribeOn(single()).observeOn(main()).doOnComplete(onMain::run);
    }
}
