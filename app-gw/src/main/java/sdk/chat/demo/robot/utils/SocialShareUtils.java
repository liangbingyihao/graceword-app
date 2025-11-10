package sdk.chat.demo.robot.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import sdk.chat.demo.pre.R;

public class SocialShareUtils {
    public static void shareHtmlLinkWithPreview(Context context, String text, String dstUrl) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        // 同时支持文本和图片
        shareIntent.setType("*/*");

//        // 构建HTML内容
//        String fullHtml = "<!DOCTYPE html><html><head><meta property=\"og:title\" content=\"" + title + "\">" +
//                "<meta property=\"og:image\" content=\"https://cdn.grace-word.com/assets/icons/app-logo.webp\">" +
//                "</head><body>" + htmlContent + "</body></html>";
//
//        // 纯文本版本
//        String fullPlainText = title + "\n\n" + plainText + "\n\n查看更多: https://www.google.com/";
        // 纯文本版本
        String title = "恩语分享";
        String fullPlainText = title + "\n\n" + text + "\n\n" + dstUrl;

        shareIntent.putExtra(Intent.EXTRA_TEXT, fullPlainText);
//        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, fullHtml);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri); // 本地预览图
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

//        // 处理FileProvider权限
//        List<ResolveInfo> resInfoList = context.getPackageManager()
//                .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
//
//        for (ResolveInfo resolveInfo : resInfoList) {
//            String packageName = resolveInfo.activityInfo.packageName;
//            context.grantUriPermission(packageName, imageUri,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "分享到"));
        } catch (Exception e) {
            Toast.makeText(context, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


//    public static Uri getDrawableUri(Context context, @DrawableRes int drawableId) {
//        // 2. 创建临时文件
//        File cachePath = new File(context.getCacheDir(), "card_cache");
//        if (!cachePath.exists()) {
//            cachePath.mkdirs();
//        }
//        File file = new File(cachePath, "ss_preview.png");
//        if (!file.exists() || file.length() == 0) {
//            // 1. 获取 drawable 并转换为 bitmap
//            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
//            if (bitmap == null) {
//                return null;
//            }
//            try {
//                // 3. 保存 bitmap 到文件
//                FileOutputStream stream = new FileOutputStream(file);
//                boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                stream.close();
//
//                if (!success) {
//                    file.delete();
//                    return null;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                return null;
//            }
//
//        }
//        // 4. 获取 Uri
//        return FileProvider.getUriForFile(context,
//                context.getPackageName() + ".provider",
//                file);
//
//    }


    public static void showCustomShareDialog(final Context context,
                                             final String[] targetPackages,
                                             final String text,
                                             final Uri imageUri, final String dstUrl) {

        // 获取包管理器
        final PackageManager pm = context.getPackageManager();

        // 准备应用列表
        final List<AppInfo> appList = new ArrayList<>();

        for (String packageName : targetPackages) {
            try {
                // 检查应用是否安装
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

                AppInfo app = new AppInfo();
                app.packageName = packageName;
                app.name = pm.getApplicationLabel(ai).toString();
                app.icon = pm.getApplicationIcon(ai);

                appList.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                // 应用未安装，跳过
            }
        }

        // 如果没有可用应用
        if (appList.isEmpty()) {
//            Toast.makeText(context, "没有找到可用的分享应用", Toast.LENGTH_SHORT).show();
            shareHtmlLinkWithPreview(context, text, dstUrl);
            return;
        }
        {
            AppInfo app = new AppInfo();
            app.packageName = "System";
            app.name = "Other";
            appList.add(app);
        }

        // 创建自定义对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("分享到");

        // 创建适配器
        ArrayAdapter<AppInfo> adapter = new ArrayAdapter<AppInfo>(
                context,
                R.layout.item_share_app,  // 自定义布局
                appList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context)
                            .inflate(R.layout.item_share_app, parent, false);
                }

                AppInfo app = getItem(position);

                ImageView icon = convertView.findViewById(R.id.app_icon);
                TextView name = convertView.findViewById(R.id.app_name);
                if(app.icon!=null){
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageDrawable(app.icon);
                }else{
                    icon.setVisibility(View.GONE);
                }
                name.setText(app.name);

                return convertView;
            }
        };

        // 设置列表点击事件
        builder.setAdapter(adapter, (dialog, which) -> {
            AppInfo selectedApp = appList.get(which);
            shareToApp(context, selectedApp.packageName, text, imageUri, dstUrl);
        });

        builder.show();
    }

    // 应用信息封装类
    private static class AppInfo {
        String packageName;
        String name;
        Drawable icon;
    }

    // 常用社交应用包名
    public static class SocialApps {
        public static final String FACEBOOK = "com.facebook.katana";
        public static final String TWITTER = "com.twitter.android";
        public static final String WHATSAPP = "com.whatsapp";
        public static final String TELEGRAM = "org.telegram.messenger";
        public static final String LINE = "jp.naver.line.android";
        public static final String FACEBOOK_LITE = "com.facebook.lite";

        // Instagram
        public static final String INSTAGRAM = "com.instagram.android";

        public static final String TWITTER_LITE = "com.twitter.android.lite";
        public static final String WHATSAPP_BUSINESS = "com.whatsapp.w4b";

        // Snapchat
        public static final String SNAPCHAT = "com.snapchat.android";

        // LinkedIn
        public static final String LINKEDIN = "com.linkedin.android";

        // Pinterest
        public static final String PINTEREST = "com.pinterest";

        // Reddit
        public static final String REDDIT = "com.reddit.frontpage";

        // TikTok
        public static final String TIKTOK = "com.zhiliaoapp.musically";

    }

    public static String[] targetApps = {
            SocialApps.FACEBOOK,
            SocialApps.FACEBOOK_LITE,
            SocialApps.TWITTER,
            SocialApps.TWITTER_LITE,
            SocialApps.LINE,
            SocialApps.WHATSAPP,
            SocialApps.TELEGRAM,
            SocialApps.PINTEREST,
            SocialApps.INSTAGRAM,
            SocialApps.REDDIT,
            SocialApps.SNAPCHAT,
    };

    // 分享到指定应用
    private static void shareToApp(Context context, String packageName,
                                   String text, Uri imageUri, String dstUrl) {

        if (SocialApps.TWITTER.equals(packageName)) {
            try {
                String tweetUrl = "https://twitter.com/intent/tweet?text=" +
                        URLEncoder.encode(text, "UTF-8") +
                        "&url=" + URLEncoder.encode(dstUrl, "UTF-8");

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
                intent.setPackage(packageName);
                context.startActivity(intent);
                return;
            } catch (Exception e) {
            }
        } else if (SocialApps.FACEBOOK.equals(packageName)) {
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, dstUrl);
                intent.setPackage(packageName);

                // 添加引用文字
                intent.putExtra(Intent.EXTRA_TITLE, text);
                context.startActivity(intent);
                return;
            } catch (Exception e) {
            }
        }else if("System".equals(packageName)){
            shareHtmlLinkWithPreview(context, text, dstUrl);
            return;
        }


        Intent intent = new Intent(Intent.ACTION_SEND);

        // 同时支持文本和图片
        intent.setType("*/*");

//        // 构建HTML内容
//        String fullHtml = "<!DOCTYPE html><html><head><meta property=\"og:title\" content=\"恩语分享\">" +
//                "<meta property=\"og:image\" content=\"https://cdn.grace-word.com/assets/icons/app-logo.webp\">" +
//                "</head><body>" + html + "</body></html>";

        // 纯文本版本
        String title = "恩语分享";
        String fullPlainText = "\n" + text + "\n\n" + dstUrl;

        intent.putExtra(Intent.EXTRA_TEXT, fullPlainText);
//        intent.putExtra(Intent.EXTRA_HTML_TEXT, fullHtml);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage(packageName);


//        // 特殊处理微信朋友圈
//        if (packageName.equals(SocialApps.WECHAT_MOMENTS)) {
//            intent.setClassName(SocialApps.WECHAT,
//                    "com.tencent.mm.ui.tools.ShareToTimeLineUI");
//            intent.putExtra("Kdescription", text);
//        }

        // 处理URI权限
        if (imageUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, imageUri); // 本地预览图
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission(packageName, imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }
}
