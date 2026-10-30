package com.suji.app.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 检查更新：对接 GitHub Releases 的「latest」发布。 */
public final class Updater {

    private Updater() {
    }

    public static final class Info {
        public boolean ok;
        public String error = "";
        /** 纯版本号，例如 "1.0.1"。 */
        public String version = "";
        public String title = "";
        public String changelog = "";
        public String releaseUrl = "";
        public String apkUrl = "";
    }

    /** 把用户填的地址规整成 GitHub API 地址。支持 https://github.com/OWNER/REPO 或完整 API 地址。 */
    public static String apiUrl(String repo) {
        String s = repo == null ? "" : repo.trim();
        if (s.isEmpty()) {
            return "";
        }
        s = s.replaceAll("/+$", "");
        if (s.contains("api.github.com/repos/")) {
            return s.contains("/releases") ? s : s + "/releases/tags/latest";
        }
        int idx = s.indexOf("github.com/");
        if (idx >= 0) {
            String path = s.substring(idx + "github.com/".length()).replaceAll("^/+", "");
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                return "https://api.github.com/repos/" + parts[0] + "/" + parts[1] + "/releases/tags/latest";
            }
        }
        return s;
    }

    public static Info check(String repoUrl) {
        Info info = new Info();
        String api = apiUrl(repoUrl);
        if (api.isEmpty()) {
            info.error = "还没有填写更新仓库地址";
            return info;
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(api).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "Suji-App");
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = readAll(in);
            if (code == 404) {
                info.error = "还没发布过版本（GitHub 上找不到 latest 发布）";
                return info;
            }
            if (code < 200 || code >= 300) {
                info.error = "检查失败：HTTP " + code;
                return info;
            }
            JSONObject json = new JSONObject(body);
            info.title = json.optString("name", "");
            String tag = json.optString("tag_name", "");
            info.version = stripV(info.title.isEmpty() ? tag : info.title);
            info.changelog = json.optString("body", "");
            info.releaseUrl = json.optString("html_url", "");
            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.optJSONObject(i);
                    if (a == null) {
                        continue;
                    }
                    String name = a.optString("name", "");
                    if (name.toLowerCase().endsWith(".apk")) {
                        info.apkUrl = a.optString("browser_download_url", "");
                        break;
                    }
                }
            }
            info.ok = true;
        } catch (Exception e) {
            info.error = "网络请求失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return info;
    }

    public interface ProgressListener {
        void onProgress(int percent);
    }

    /** 下载 APK 到指定目录，返回文件。失败抛异常。 */
    public static File downloadApk(String url, File dir, ProgressListener listener) throws Exception {
        if (url == null || url.isEmpty()) {
            throw new Exception("下载地址为空");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建下载目录");
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "Suji-App");
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            conn.disconnect();
            throw new Exception("下载失败：HTTP " + code);
        }
        int total = conn.getContentLength();
        File file = new File(dir, "suji-update.apk");
        InputStream in = conn.getInputStream();
        FileOutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[8192];
        int read;
        int done = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            done += read;
            if (listener != null && total > 0) {
                int pct = (int) (done * 100L / total);
                listener.onProgress(Math.max(0, Math.min(100, pct)));
            }
        }
        out.close();
        in.close();
        conn.disconnect();
        return file;
    }

    private static String stripV(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.startsWith("v") || t.startsWith("V")) {
            t = t.substring(1).trim();
        }
        return t;
    }

    /** 比较版本号，a > b 返回 1，相等 0，a < b 返回 -1。 */
    public static int compareVersion(String a, String b) {
        String[] pa = (a == null ? "" : a).split("[^0-9]+");
        String[] pb = (b == null ? "" : b).split("[^0-9]+");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int va = 0;
            int vb = 0;
            if (i < pa.length && !pa[i].isEmpty()) {
                va = parseIntSafe(pa[i]);
            }
            if (i < pb.length && !pb[i].isEmpty()) {
                vb = parseIntSafe(pb[i]);
            }
            if (va != vb) {
                return va > vb ? 1 : -1;
            }
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        r.close();
        return sb.toString();
    }
}
