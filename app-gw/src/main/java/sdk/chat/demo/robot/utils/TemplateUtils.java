package sdk.chat.demo.robot.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import sdk.chat.demo.robot.api.model.Song;

public class TemplateUtils {

    public static String loadTemplate(Context context, String templatePath) throws IOException {
        InputStream is = context.getAssets().open(templatePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        is.close();
        return builder.toString();
    }

    public static String buildSongHtml(Song song) {
        StringBuilder sb = new StringBuilder();

        sb.append("<div class=\"message-block assistant\">\n");
        sb.append("    \n");
        sb.append("    <div class=\"message-content hymn-container\">\n");

        // 歌曲标题
        sb.append("        <div class=\"hymn-title\">").append(escapeHtml(getSongTitle(song))).append("</div>\n");
        sb.append("\n");

        // 操作按钮区域
        sb.append("        <div class=\"hymn-actions\">\n");

        // 收听链接
        if (song.getListenUrl() != null && !song.getListenUrl().isEmpty()) {
            sb.append("            <a href=\"").append(escapeHtml(song.getListenUrl())).append("\" class=\"hymn-action\" target=\"_blank\">收听</a>\n");
        }

        // 分隔符逻辑
        boolean hasPreviousAction = song.getListenUrl() != null && !song.getListenUrl().isEmpty();

        // 乐谱链接
        if (song.getSheetMusicUrl() != null && !song.getSheetMusicUrl().isEmpty()) {
            if (hasPreviousAction) {
                sb.append("            <span class=\"hymn-action-separator\">|</span>\n");
            }
            sb.append("            <a href=\"").append(escapeHtml(song.getSheetMusicUrl())).append("\" class=\"hymn-action\" target=\"_blank\">乐谱</a>\n");
            hasPreviousAction = true;
        }

        // PPT链接
        if (song.getPptUrl() != null && !song.getPptUrl().isEmpty()) {
            if (hasPreviousAction) {
                sb.append("            <span class=\"hymn-action-separator\">|</span>\n");
            }
            sb.append("            <a href=\"").append(escapeHtml(song.getPptUrl())).append("\" class=\"hymn-action\" target=\"_blank\">歌词PPT</a>\n");
        }

        sb.append("        </div>\n");
        sb.append("\n");

        // 作曲作词信息
        if ((song.getComposer() != null && !song.getComposer().isEmpty()) ||
                (song.getLyricist() != null && !song.getLyricist().isEmpty())) {
            sb.append("        <div class=\"hymn-metadata\">\n");

            if (song.getComposer() != null && !song.getComposer().isEmpty()) {
                sb.append("            作曲：").append(escapeHtml(song.getComposer()));
            }

            if (song.getComposer() != null && !song.getComposer().isEmpty() &&
                    song.getLyricist() != null && !song.getLyricist().isEmpty()) {
                sb.append("&nbsp;\n");
            }

            if (song.getLyricist() != null && !song.getLyricist().isEmpty()) {
                sb.append("            作词：").append(escapeHtml(song.getLyricist()));
            }

            sb.append("        </div>\n");
        }
        sb.append("\n");

        // 专辑信息
        if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
            sb.append("        <div class=\"hymn-metadata\">专辑：").append(escapeHtml(song.getAlbum())).append("</div>\n");
            sb.append("\n");
        }

        // 歌词区域
        if (song.getLyrics() != null && !song.getLyrics().isEmpty()) {
            sb.append("        <div class=\"hymn-lyrics-wrapper\">\n");
            sb.append("            <div class=\"hymn-lyrics collapsed\">").append(escapeHtml(song.getLyrics())).append("</div>\n");
            sb.append("            <a class=\"hymn-toggle-button\" style=\"display: none;\">全文</a>\n");
            sb.append("        </div>\n");
            sb.append("\n");
        }

        // 版权信息
        if (song.getCopyright() != null && !song.getCopyright().isEmpty()) {
            sb.append("        <div class=\"hymn-copyright\">").append(escapeHtml(song.getCopyright())).append("</div>\n");
            sb.append("\n");
        }

        sb.append("    </div>\n");
        sb.append("    \n");
        sb.append("</div>");

        return sb.toString();
    }

    /**
     * 获取歌曲标题（优先使用英文标题）
     */
    private static String getSongTitle(Song song) {
        if (song.getEnglishTitle() != null && !song.getEnglishTitle().isEmpty()) {
            return song.getEnglishTitle();
        }
        return song.getTitle();
    }

    /**
     * HTML 转义函数
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
