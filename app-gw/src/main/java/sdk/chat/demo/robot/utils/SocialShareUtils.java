package sdk.chat.demo.robot.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;

import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.R;

public class SocialShareUtils {
    public static void shareHtmlLinkWithPreview(Context context, String title, String htmlContent, String plainText, Uri imageUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        // 同时支持文本和图片
        shareIntent.setType("*/*");

        // 构建HTML内容
        String fullHtml = "<!DOCTYPE html><html><head><meta property=\"og:title\" content=\"" + title + "\">" +
                "<meta property=\"og:image\" content=\"https://cdn.grace-word.com/assets/icons/app-logo.webp\">" +
                "</head><body>" + htmlContent + "</body></html>";

        // 纯文本版本
        String fullPlainText = title + "\n\n" + plainText + "\n\n查看更多: https://www.google.com/";

        shareIntent.putExtra(Intent.EXTRA_TEXT, fullPlainText);
        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, fullHtml);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri); // 本地预览图
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 处理FileProvider权限
        List<ResolveInfo> resInfoList = context.getPackageManager()
                .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "分享到"));
        } catch (Exception e) {
            Toast.makeText(context, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public static Uri getDrawableUri(Context context, @DrawableRes int drawableId) {
        // 2. 创建临时文件
        File cachePath = new File(context.getCacheDir(), "card_cache");
        if (!cachePath.exists()) {
            cachePath.mkdirs();
        }
        File file = new File(cachePath, "ss_preview.png");
        if (!file.exists() || file.length() == 0) {
            // 1. 获取 drawable 并转换为 bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
            if (bitmap == null) {
                return null;
            }
            try {
                // 3. 保存 bitmap 到文件
                FileOutputStream stream = new FileOutputStream(file);
                boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

                if (!success) {
                    file.delete();
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // 4. 获取 Uri
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider",
                file);

    }

    private static boolean isAppAvailable(Context context, String packageName, String type) {

        PackageManager pm = MainApp.getContext().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type);
        intent.setPackage(packageName);

        return intent.resolveActivity(pm) != null;

    }


    public static void showCustomShareDialog(final Context context,
                                             final String[] targetPackages,
                                             final String text, final String html,
                                             final Uri imageUri, final String title) {

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
            Toast.makeText(context, "没有找到可用的分享应用", Toast.LENGTH_SHORT).show();
            return;
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

                icon.setImageDrawable(app.icon);
                name.setText(app.name);

                return convertView;
            }
        };

        // 设置列表点击事件
        builder.setAdapter(adapter, (dialog, which) -> {
            AppInfo selectedApp = appList.get(which);
            shareToApp(context, selectedApp.packageName, text, html, imageUri, title);
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
        public static final String WECHAT = "com.tencent.mm";
        public static final String WECHAT_MOMENTS = "com.tencent.mm";
        public static final String QQ = "com.tencent.mobileqq";
        public static final String WEIBO = "com.sina.weibo";
        public static final String FACEBOOK = "com.facebook.katana";
        public static final String TWITTER = "com.twitter.android";
        public static final String WHATSAPP = "com.whatsapp";
        public static final String TELEGRAM = "org.telegram.messenger";
        public static final String LINE = "jp.naver.line.android";
    }

    public static String[] targetApps = {
            SocialApps.FACEBOOK,
            SocialApps.TWITTER,
            SocialApps.WHATSAPP,
            SocialApps.TELEGRAM,
            "app.graceword.android"
    };

    // 分享到指定应用
    private static void shareToApp(Context context, String packageName,
                                   String text, String html, Uri imageUri, String title) {




        Intent intent = new Intent(Intent.ACTION_SEND);

        // 同时支持文本和图片
        intent.setType("*/*");

        // 构建HTML内容
        String fullHtml = "<!DOCTYPE html><html><head><meta property=\"og:title\" content=\"" + title + "\">" +
                "<meta property=\"og:image\" content=\"https://cdn.grace-word.com/assets/icons/app-logo.webp\">" +
                "</head><body>" + html + "</body></html>";

        // 纯文本版本
        String fullPlainText = title + "\n\n" + text + "\n\n查看更多: https://www.google.com/";

        intent.putExtra(Intent.EXTRA_TEXT, fullPlainText);
        intent.putExtra(Intent.EXTRA_HTML_TEXT, fullHtml);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri); // 本地预览图
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage(packageName);

//        if (imageUri != null) {
//            intent.setType("image/*");
//            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
//        } else {
//            intent.setType("text/plain");
//        }
//
//        intent.putExtra(Intent.EXTRA_TEXT, text);
//        intent.putExtra(Intent.EXTRA_HTML_TEXT, html);
//        intent.putExtra(Intent.EXTRA_SUBJECT, title);

//
//        shareIntent.putExtra(Intent.EXTRA_TEXT, fullPlainText);
//        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, fullHtml);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri); // 本地预览图
//        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        // 处理FileProvider权限
//        List<ResolveInfo> resInfoList = context.getPackageManager()
//                .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
//
//        for (ResolveInfo resolveInfo : resInfoList) {
//            String packageName = resolveInfo.activityInfo.packageName;
//            context.grantUriPermission(packageName, imageUri,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        }
//

        // 特殊处理微信朋友圈
        if (packageName.equals(SocialApps.WECHAT_MOMENTS)) {
            intent.setClassName(SocialApps.WECHAT,
                    "com.tencent.mm.ui.tools.ShareToTimeLineUI");
            intent.putExtra("Kdescription", text);
        }

        // 处理URI权限
        if (imageUri != null) {
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
