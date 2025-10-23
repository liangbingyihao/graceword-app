package sdk.chat.demo.robot.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
}
