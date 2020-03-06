package com.drcuiyutao.idea.plugin.util;

public class LogUtil {
    public static void i(String tag, String msg) {
        System.out.println("[" + tag + "] [" + msg + "]");
    }

    public static void e(String tag, String msg) {
        System.err.println("[" + tag + "] [" + msg + "]");
    }
}
