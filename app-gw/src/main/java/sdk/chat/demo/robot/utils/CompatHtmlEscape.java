package sdk.chat.demo.robot.utils;

import android.os.Build;
import android.text.Html;

public class CompatHtmlEscape {
    public static String escapeHtmlCompat(String text) {
        if (text == null) return "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // API 16+ 使用系统方法
            return Html.escapeHtml(text);
        } else {
            // 低版本使用自定义实现
            return escapeHtmlLegacy(text);
        }
    }

    private static String escapeHtmlLegacy(String text) {
        if (text == null) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
