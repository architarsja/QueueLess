package com.queueless.util;

public final class JsonUtil {
    private JsonUtil() {}
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    public static String obj(String... pairs) {
        StringBuilder b = new StringBuilder("{");
        for (int i=0;i<pairs.length;i+=2) { if(i>0)b.append(','); b.append('"').append(esc(pairs[i])).append("\":\"").append(esc(pairs[i+1])).append('"'); }
        return b.append('}').toString();
    }
    public static String ok(String message) { return obj("success","true","message",message); }
    public static String error(String message) { return obj("success","false","message",message); }
}
