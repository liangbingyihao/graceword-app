package sdk.chat.demo.robot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.gyf.immersionbar.ImmersionBar;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import sdk.chat.core.dao.User;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.KeyValuePair;
import sdk.chat.demo.robot.extensions.FirebaseReport;
import sdk.chat.demo.robot.handlers.GWAuthenticationHandler;
import sdk.chat.demo.robot.handlers.LogUploader;
import sdk.chat.demo.robot.utils.ToastHelper;
import sdk.guru.common.RX;

public class GuideActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private MaterialButton btnNext;
    private final GuideImage[] guideImages = {
            new GuideImage(R.mipmap.ic_intro_1, null),
            new GuideImage(R.mipmap.ic_intro_2, null),
//            R.mipmap.ic_intro_m3
    };
    private final GuideImage[] guideImagesHK = {
            new GuideImage(R.mipmap.ic_intro_1_hk, null),
            new GuideImage(R.mipmap.ic_intro_2_hk, null),
//            R.mipmap.ic_intro_m3
    };
    private final GuideImage[] guideImagesEN = {
            new GuideImage(R.mipmap.ic_intro_1_en, null),
            new GuideImage(R.mipmap.ic_intro_2_en, null),
//            R.mipmap.ic_intro_m3
    };
    private final int[] guideTitles = {
            R.string.guide_1,
            R.string.guide_2,
            R.string.guide_3,
    };
    private final int[] guideDescriptions = {
            R.string.guide_desc_1,
            R.string.guide_desc_2,
            R.string.guide_desc_3,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this).init();
        setContentView(R.layout.activity_guide);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.layoutDots);
//        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        // 设置适配器

        var lang = Locale.getDefault().toLanguageTag().toLowerCase();

        List<GuideImage> list = null;

        if (lang.contains("en")) {
            list = new ArrayList<>(Arrays.asList(guideImagesEN));
        } else if (lang.contains("hant")) {
            list = new ArrayList<>(Arrays.asList(guideImagesHK));
        } else {
            list = new ArrayList<>(Arrays.asList(guideImages));
        }
        var configs = ImageApi.getGwConfigs();
        if (configs != null && configs.getWelcomeSurvey() != null) {
            String eventPage = configs.getWelcomeSurvey().getEventPage();
            if (eventPage != null && !eventPage.isEmpty()) {
                list.add(new GuideImage(0, eventPage));
            }
        }
        viewPager.setAdapter(new GuideViewAdapter(list));

        // 添加指示点
//        addDots(0);

        // 设置ViewPager页面改变监听
        viewPager.setOffscreenPageLimit(2);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
//                addDots(position);
                // 改变按钮文字
                btnNext.setText(getString(R.string.next));
//                    btnSkip.setVisibility(View.GONE);
//                } else {
//                    btnNext.setText(getString(R.string.next));
////                    btnSkip.setVisibility(View.VISIBLE);
//                }
                if (position == guideImages.length) {
                    var configs = ImageApi.getGwConfigs();
                    if (configs != null && configs.getWelcomeSurvey() != null && configs.getWelcomeSurvey().getBackground() != null) {
                        String url = configs.getWelcomeSurvey().getBackground();
                        if (url != null && !url.isEmpty()) {
                            Log.e("guide", "preload:" + url);
                            Glide.with(MainApp.getContext())
                                    .load(url)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload();
                        }
                    }
                }
            }
        });

//        btnSkip.setOnClickListener(v -> launchMainActivity());

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < viewPager.getAdapter().getItemCount() - 1) {
                // 移动到下一页
                viewPager.setCurrentItem(current + 1);
            } else {
                launchMainActivity();
            }

            LogUploader.reportEvent(
                    "mod_guide", List.of(
                            new KeyValuePair("guide_action", "10"),
                            new KeyValuePair("guide_type", "app_launch")
                    )
            );
        });
        LogUploader.reportEvent(
                "mod_guide", List.of(
                        new KeyValuePair("guide_action", "0"),
                        new KeyValuePair("guide_type", "app_launch")
                )
        );
    }

    @Override
    protected int getLayout() {
        return 0;
    }

//    private void addDots(int currentPosition) {
//        dotsLayout.removeAllViews();
//
//        for (int i = 0; i < guideImages.length; i++) {
//            View dot = new View(this);
//            dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_unselected));
//
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    dpToPx(8), dpToPx(8));
//            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
//            dot.setLayoutParams(params);
//
//            if (i == currentPosition) {
//                dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_selected));
//            }
//
//            dotsLayout.addView(dot);
//        }
//    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean retrying = false;

    private void launchMainActivity() {
        MainApp app = (MainApp) getApplication();

        User me = null;
        try {
            me = ChatSDK.currentUser();
        } catch (Exception e) {
            Logger.error(e, "currentUser error");
            me = null;
        }
        if (app.isInitialized() && me != null) {
            startActivity(new Intent(this, MainDrawerActivity.class));
            finish();
        } else if (!retrying) {
            retrying = true;
            btnNext.setText(getString(R.string.retrying));
            ToastHelper.show(this, R.string.network_error);
            dm.add(ChatSDK.auth().authenticate()
                    .observeOn(RX.main())
                    .doFinally(() -> {
                        btnNext.setText(getString(R.string.retry));
                        GWAuthenticationHandler.ensureDatabase();
                        retrying = false;
                    })
                    .subscribe(
                            () -> {
                                startActivity(new Intent(this, MainDrawerActivity.class));
                                finish();
                                Logger.error("guide.authenticate done");
                            },
                            error -> { /* 错误处理 */
                                Logger.error(error, "guide.authenticate error");
                                retrying = false;
                                btnNext.setText(getString(R.string.retry));
                                ToastHelper.show(this, R.string.network_error);
                                FirebaseReport.INSTANCE.reportExportEvent("app.init", "authenticate error", error);
                            }
                    ));
        }
    }

    class GuideImage {
        int resId;
        String url;

        public GuideImage(int resId, String url) {
            this.resId = resId;
            this.url = url;
        }
    }

    class GuideViewAdapter extends RecyclerView.Adapter<GuideViewAdapter.ViewHolder> {
        private List<GuideImage> guideImages;

        public GuideViewAdapter(List<GuideImage> images) {
            this.guideImages = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guide2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GuideImage image = guideImages.get(position);
            if (image.resId > 0) {
                holder.imageView.setImageResource(image.resId);
            } else if (image.url != null && !image.url.isEmpty()) {
                Glide.with(GuideActivity.this)
                        .load(image.url)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.color.bg_bill_menu)
                        .error(R.mipmap.ic_splash)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return guideImages.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
//            TextView titleView;
//            TextView descView;
//            View maskView;

            public ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image);
//                titleView = view.findViewById(R.id.title);
//                descView = view.findViewById(R.id.description);
//                maskView = view.findViewById(R.id.image_mask);
            }
        }
    }
}