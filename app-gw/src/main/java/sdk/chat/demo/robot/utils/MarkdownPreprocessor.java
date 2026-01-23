package sdk.chat.demo.robot.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownPreprocessor {

    /**
     * 批量处理无空格的 **内容** 模式
     */
    public static String preprocessMultipleBoldMarkers(String text) {
        if (text == null) return "";

        // 匹配所有 **内容** 模式，其中内容不包含 * 号
        // 使用非贪婪匹配 .*? 来处理多个实例
        Pattern pattern = Pattern.compile("\\*\\*([^*]+?)\\*\\*");
        Matcher matcher = pattern.matcher(text);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(1);
            // 检查前后字符，只在需要时添加空格
            String replacement = getSpacedReplacement(text, matcher.start(), matcher.end(), content);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String getSpacedReplacement(String text, int start, int end, String content) {
        boolean needsLeadingSpace = start > 0 && !Character.isWhitespace(text.charAt(start - 1));
        boolean needsTrailingSpace = end < text.length() && !Character.isWhitespace(text.charAt(end));

        StringBuilder replacement = new StringBuilder();
        if (needsLeadingSpace) replacement.append(" ");
        replacement.append("**").append(content).append("**");
        if (needsTrailingSpace) replacement.append(" ");

        return replacement.toString();
    }
}