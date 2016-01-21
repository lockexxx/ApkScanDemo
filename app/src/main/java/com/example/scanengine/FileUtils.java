package com.example.scanengine;

import android.text.TextUtils;

import java.io.File;

/**
 * @author locke
 */
public class FileUtils {

    public static String addSlash(final String path) {
        if (TextUtils.isEmpty(path)) {
            return File.separator;
        }

        if (path.charAt(path.length() - 1) != File.separatorChar) {
            return path + File.separatorChar;
        }

        return path;
    }
}
