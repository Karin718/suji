package com.suji.app.util;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 通过 SAF 读写文本文件。 */
public final class FileIo {

    private FileIo() {
    }

    public static String readText(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) {
            throw new Exception("无法打开文件");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        in.close();
        byte[] bytes = bos.toByteArray();

        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (utf8.indexOf('\uFFFD') >= 0) {
            // 可能是 GBK 编码的旧文件
            try {
                String gbk = new String(bytes, Charset.forName("GBK"));
                if (gbk.indexOf('\uFFFD') < 0) {
                    return gbk;
                }
            } catch (Exception ignored) {
                // 忽略
            }
        }
        return utf8;
    }

    public static void writeText(Context context, Uri uri, String text) throws Exception {
        OutputStream out = context.getContentResolver().openOutputStream(uri, "wt");
        if (out == null) {
            throw new Exception("无法写入文件");
        }
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
