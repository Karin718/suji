package com.suji.app.util;

import com.suji.app.ai.AiParser;
import com.suji.app.data.Card;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把"原始资料"解析成卡片。
 *
 * <p>支持三种常见写法：</p>
 * <pre>
 * abandon ---- v. 放弃
 * abandon：v. 放弃
 * abandon    v. 放弃        （用 Tab 分隔）
 * </pre>
 * <p>也支持"正面一行、背面接在后面"的段落写法（空行分块）。</p>
 */
public final class Parser {

    private static final String[] SEPS = {"----", "——", "||", "\t", "|", "：", "＝", "=", "→", ":"};

    private Parser() {
    }

    public static List<Card> parse(String raw) {
        List<Card> out = new ArrayList<>();
        String text = normalize(raw);
        if (text.isEmpty()) {
            return out;
        }

        String[] lines = text.split("\n");
        int nonEmpty = 0;
        int withSep = 0;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            nonEmpty++;
            if (indexOfSep(line) >= 0) {
                withSep++;
            }
        }

        if (nonEmpty == 0) {
            return out;
        }

        if (withSep * 2 >= nonEmpty) {
            // 一行一张卡
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    addLine(line, out);
                }
            }
        } else {
            // 空行分块：第一行正面，其余行背面
            String[] blocks = text.split("\n[ \t]*\n");
            for (String block : blocks) {
                String b = block.trim();
                if (b.isEmpty()) {
                    continue;
                }
                String[] bl = b.split("\n");
                if (bl.length == 1) {
                    addLine(bl[0], out);
                    continue;
                }
                Card card = new Card();
                card.front = clean(bl[0]);
                StringBuilder back = new StringBuilder();
                for (int i = 1; i < bl.length; i++) {
                    if (back.length() > 0) {
                        back.append('\n');
                    }
                    back.append(bl[i].trim());
                }
                card.back = back.toString().trim();
                if (card.front.isEmpty()) {
                    card.front = card.back;
                    card.back = "";
                }
                out.add(card);
            }
        }
        return dedupe(out);
    }

    /** 逗号分隔的表格（.csv）。 */
    public static List<Card> parseCsv(String text) {
        List<Card> out = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        String t = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        int i = 0;
        while (i < t.length()) {
            char ch = t.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < t.length() && t.charAt(i + 1) == '"') {
                        cell.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                cell.append(ch);
                i++;
                continue;
            }
            if (ch == '"') {
                inQuotes = true;
                i++;
                continue;
            }
            if (ch == ',') {
                row.add(cell.toString());
                cell.setLength(0);
                i++;
                continue;
            }
            if (ch == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                i++;
                continue;
            }
            cell.append(ch);
            i++;
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }

        int start = 0;
        if (!rows.isEmpty() && isHeader(rows.get(0))) {
            start = 1;
        }
        for (int r = start; r < rows.size(); r++) {
            List<String> cols = rows.get(r);
            if (cols.isEmpty()) {
                continue;
            }
            String front = clean(cols.get(0));
            String back = cols.size() > 1 ? clean(cols.get(1)) : "";
            String note = cols.size() > 2 ? cols.get(2).trim() : "";
            String tags = cols.size() > 3 ? cols.get(3).trim() : "";
            if (front.isEmpty() && back.isEmpty()) {
                continue;
            }
            Card card = new Card();
            card.front = front;
            card.back = back;
            card.note = note;
            card.tags = tags;
            out.add(card);
        }
        return dedupe(out);
    }

    /** JSON 数组：[{"front":"..","back":".."}]，也接受 {"cards":[...]}。 */
    public static List<Card> parseJson(String text) {
        List<Card> out = AiParser.parse(text);
        if (out.isEmpty() && text != null) {
            try {
                JSONObject obj = new JSONObject(text.trim());
                JSONArray arr = obj.optJSONArray("cards");
                if (arr != null) {
                    out = AiParser.parse(arr.toString());
                }
            } catch (Exception ignored) {
                // 忽略
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 内部实现

    private static void addLine(String line, List<Card> out) {
        int idx = indexOfSep(line);
        if (idx > 0) {
            String sep = sepAt(line, idx);
            Card card = new Card();
            card.front = clean(line.substring(0, idx));
            card.back = clean(line.substring(idx + sep.length()));
            if (card.front.isEmpty() && card.back.isEmpty()) {
                return;
            }
            out.add(card);
        } else {
            String front = clean(line);
            if (!front.isEmpty()) {
                Card card = new Card();
                card.front = front;
                out.add(card);
            }
        }
    }

    private static int indexOfSep(String line) {
        int best = -1;
        String bestSep = null;
        for (String sep : SEPS) {
            int idx = line.indexOf(sep);
            if (idx > 0 && (best < 0 || idx < best)) {
                best = idx;
                bestSep = sep;
            }
        }
        return bestSep == null ? -1 : best;
    }

    private static String sepAt(String line, int idx) {
        for (String sep : SEPS) {
            if (line.startsWith(sep, idx)) {
                return sep;
            }
        }
        return line.substring(idx, idx + 1);
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.replace("\r\n", "\n").replace('\r', '\n');
        t = t.replace('\u3000', ' ');
        return t.trim();
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        t = t.replaceAll("^\\s*(?:\\d+\\s*[.、)．]|[-*•])\\s*", "");
        t = t.replaceAll("[ \\t]+", " ");
        return t.trim();
    }

    private static List<Card> dedupe(List<Card> cards) {
        Set<String> seen = new HashSet<>();
        List<Card> out = new ArrayList<>();
        for (Card c : cards) {
            String key = c.front + "\u0001" + c.back;
            if (c.front.isEmpty() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            out.add(c);
        }
        return out;
    }

    private static boolean isHeader(List<String> row) {
        if (row.isEmpty()) {
            return false;
        }
        String first = row.get(0).trim().toLowerCase();
        if (first.isEmpty()) {
            return false;
        }
        return first.equals("front") || first.equals("term") || first.equals("word")
                || first.contains("正面") || first.contains("单词") || first.contains("问题");
    }
}
