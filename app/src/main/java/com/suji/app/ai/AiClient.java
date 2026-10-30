package com.suji.app.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 极简的 OpenAI 兼容接口客户端（DeepSeek / 通义 / OpenAI / 本地 Ollama 都能用）。 */
public final class AiClient {

    private AiClient() {
    }

    public static final class Result {
        public boolean ok;
        public String text = "";
        public String error = "";

        static Result ok(String text) {
            Result r = new Result();
            r.ok = true;
            r.text = text;
            return r;
        }

        static Result fail(String message) {
            Result r = new Result();
            r.ok = false;
            r.error = message;
            return r;
        }
    }

    public static Result chat(String baseUrl, String apiKey, String model, String system, String user) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Result.fail("还没有填写 API Key，请到「设置 → AI 整理」里填写。");
        }
        if (model == null || model.trim().isEmpty()) {
            return Result.fail("还没有填写模型名称，例如 deepseek-chat。");
        }
        String url = normalize(baseUrl);
        if (url.isEmpty()) {
            return Result.fail("接口地址不能为空，例如 https://api.deepseek.com/v1");
        }

        HttpURLConnection conn = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", model.trim());
            body.put("temperature", 0.2);
            body.put("stream", false);

            JSONArray messages = new JSONArray();
            if (system != null && !system.isEmpty()) {
                messages.put(new JSONObject().put("role", "system").put("content", system));
            }
            messages.put(new JSONObject().put("role", "user").put("content", user));
            body.put("messages", messages);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(180000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            conn.setRequestProperty("Accept", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(payload);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = readAll(stream);

            if (code < 200 || code >= 300) {
                String detail = response;
                try {
                    JSONObject err = new JSONObject(response).optJSONObject("error");
                    if (err != null) {
                        detail = err.optString("message", response);
                    }
                } catch (Exception ignored) {
                    // 保留原始返回
                }
                return Result.fail("接口返回 HTTP " + code + "：" + shorten(detail, 300));
            }

            JSONObject json = new JSONObject(response);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return Result.fail("接口返回格式异常：" + shorten(response, 300));
            }
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message == null) {
                return Result.fail("接口没有返回 message 字段：" + shorten(response, 300));
            }
            return Result.ok(message.optString("content", ""));
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return Result.fail("网络请求失败：" + msg);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String normalize(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.isEmpty()) {
            return "";
        }
        if (!url.endsWith("/chat/completions")) {
            url = url + "/chat/completions";
        }
        return url;
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
