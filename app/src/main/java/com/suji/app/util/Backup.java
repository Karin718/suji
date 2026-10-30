package com.suji.app.util;

import com.suji.app.data.Card;
import com.suji.app.data.Deck;
import com.suji.app.data.Repo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 备份导出 / 导入（完整保留复习进度）。 */
public final class Backup {

    private Backup() {
    }

    public static String exportJson(Repo repo) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", "suji");
        root.put("version", 1);
        root.put("exportedAt", System.currentTimeMillis());

        JSONArray decks = new JSONArray();
        List<Card> allCards = repo.db().cards().all();
        for (Deck deck : repo.db().decks().all()) {
            JSONObject d = new JSONObject();
            d.put("name", deck.name);
            d.put("description", deck.description);
            d.put("createdAt", deck.createdAt);

            JSONArray cards = new JSONArray();
            for (Card c : allCards) {
                if (c.deckId != deck.id) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put("front", c.front);
                o.put("back", c.back);
                o.put("note", c.note);
                o.put("tags", c.tags);
                o.put("stage", c.stage);
                o.put("dueAt", c.dueAt);
                o.put("createdAt", c.createdAt);
                o.put("lastReviewAt", c.lastReviewAt);
                o.put("firstReviewAt", c.firstReviewAt);
                o.put("reviews", c.reviews);
                o.put("lapses", c.lapses);
                cards.put(o);
            }
            d.put("cards", cards);
            decks.put(d);
        }
        root.put("decks", decks);
        return root.toString(2);
    }

    /** 返回导入的卡片数量。 */
    public static int importJson(Repo repo, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray decks = root.optJSONArray("decks");
        if (decks == null) {
            throw new JSONException("文件里没有 decks 字段，可能不是速记的备份");
        }
        int count = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < decks.length(); i++) {
            JSONObject d = decks.getJSONObject(i);
            Deck deck = new Deck(d.optString("name", "导入的背诵本"));
            deck.description = d.optString("description", "");
            deck.createdAt = d.optLong("createdAt", now + i);
            deck.id = repo.db().decks().insert(deck);

            JSONArray cards = d.optJSONArray("cards");
            if (cards == null) {
                continue;
            }
            List<Card> list = new ArrayList<>();
            for (int j = 0; j < cards.length(); j++) {
                JSONObject o = cards.getJSONObject(j);
                Card c = new Card(deck.id, o.optString("front", ""), o.optString("back", ""));
                c.note = o.optString("note", "");
                c.tags = o.optString("tags", "");
                c.stage = o.optInt("stage", 0);
                c.dueAt = o.optLong("dueAt", now);
                c.createdAt = o.optLong("createdAt", now);
                c.lastReviewAt = o.optLong("lastReviewAt", 0);
                c.firstReviewAt = o.optLong("firstReviewAt", 0);
                c.reviews = o.optInt("reviews", 0);
                c.lapses = o.optInt("lapses", 0);
                if (c.front.trim().isEmpty()) {
                    continue;
                }
                list.add(c);
            }
            if (!list.isEmpty()) {
                repo.db().cards().insertAll(list);
                count += list.size();
            }
        }
        return count;
    }
}
