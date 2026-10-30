package com.suji.app.ai;

import com.suji.app.data.Card;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 容错地从模型输出里抠出卡片数组。 */
public final class AiParser {

    private AiParser() {
    }

    public static List<Card> parse(String content) {
        List<Card> out = new ArrayList<>();
        if (content == null) {
            return out;
        }
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return out;
        }
        String json = content.substring(start, end + 1);
        JSONArray array;
        try {
            array = new JSONArray(json);
        } catch (Exception e) {
            return out;
        }
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject) {
                JSONObject obj = (JSONObject) item;
                Card card = new Card();
                card.front = pick(obj, "front", "term", "question", "q", "正面");
                card.back = pick(obj, "back", "definition", "answer", "a", "背面");
                card.note = pick(obj, "note", "notes", "explain", "备注");
                card.tags = pickTags(obj);
                if (card.front.isEmpty() && !card.back.isEmpty()) {
                    card.front = card.back;
                    card.back = "";
                }
                out.add(card);
            } else if (item != null) {
                String s = String.valueOf(item).trim();
                if (!s.isEmpty()) {
                    Card card = new Card();
                    card.front = s;
                    out.add(card);
                }
            }
        }
        return out;
    }

    private static String pick(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key)) {
                String v = obj.optString(key, "");
                if (v != null && !v.trim().isEmpty() && !"null".equals(v)) {
                    return v.trim();
                }
            }
        }
        return "";
    }

    private static String pickTags(JSONObject obj) {
        for (String key : new String[]{"tags", "tag", "标签"}) {
            if (!obj.has(key)) {
                continue;
            }
            Object v = obj.opt(key);
            if (v instanceof JSONArray) {
                JSONArray arr = (JSONArray) v;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(arr.optString(i, ""));
                }
                return sb.toString();
            }
            if (v != null) {
                return String.valueOf(v).trim();
            }
        }
        return "";
    }
}
