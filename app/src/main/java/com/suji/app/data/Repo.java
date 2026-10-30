package com.suji.app.data;

import android.content.Context;

import com.suji.app.core.Scheduler;
import com.suji.app.util.DateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 统一的业务入口。所有方法都是同步的，请在 IO 线程里调用（见 {@code Async}）。 */
public class Repo {

    private static volatile Repo INSTANCE;

    private final Context app;
    private final AppDatabase db;
    private final Prefs prefs;

    private Repo(Context context) {
        app = context.getApplicationContext();
        db = AppDatabase.get(app);
        prefs = new Prefs(app);
    }

    public static Repo get(Context context) {
        if (INSTANCE == null) {
            synchronized (Repo.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Repo(context);
                }
            }
        }
        return INSTANCE;
    }

    public AppDatabase db() {
        return db;
    }

    public Prefs prefs() {
        return prefs;
    }

    public long[] intervals() {
        return Scheduler.parse(prefs.intervals());
    }

    // ---------------------------------------------------------------- 背诵本

    /** 没有任何背诵本时创建一个默认的。 */
    public Deck ensureDeck() {
        List<Deck> decks = db.decks().all();
        if (!decks.isEmpty()) {
            return decks.get(0);
        }
        Deck d = new Deck("我的速记");
        d.description = "手动录入和导入的内容都会放在这里";
        d.id = db.decks().insert(d);
        return d;
    }

    public Deck createDeck(String name, String description) {
        Deck d = new Deck(name == null || name.trim().isEmpty() ? "未命名背诵本" : name.trim());
        d.description = description == null ? "" : description.trim();
        d.id = db.decks().insert(d);
        return d;
    }

    public void renameDeck(Deck deck, String name) {
        deck.name = name;
        db.decks().update(deck);
    }

    public void deleteDeck(Deck deck) {
        db.cards().deleteByDeck(deck.id);
        db.decks().delete(deck);
    }

    // ---------------------------------------------------------------- 卡片

    public void addCards(long deckId, List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Card c : cards) {
            c.deckId = deckId;
            if (c.createdAt == 0) {
                c.createdAt = now;
            }
            if (c.dueAt == 0) {
                c.dueAt = now;
            }
        }
        db.cards().insertAll(cards);
    }

    public void saveCard(Card card) {
        if (card.id == 0) {
            if (card.createdAt == 0) {
                card.createdAt = System.currentTimeMillis();
            }
            if (card.dueAt == 0) {
                card.dueAt = card.createdAt;
            }
            card.id = db.cards().insert(card);
        } else {
            db.cards().update(card);
        }
    }

    public void deleteCard(Card card) {
        db.cards().delete(card);
    }

    public void resetCard(Card card) {
        db.cards().resetProgress(card.id, System.currentTimeMillis());
    }

    // ---------------------------------------------------------------- 学习

    /**
     * 生成今天的学习队列：到期的旧卡 + 未学的新卡（受"每日新学上限"约束），并按 3:1 交错。
     *
     * @param deckId 指定背诵本；小于 0 表示全部
     */
    public List<Card> buildQueue(long deckId) {
        long now = System.currentTimeMillis();
        long from = DateUtil.startOfDay(now);
        long to = DateUtil.startOfNextDay(now);

        int introducedToday = db.cards().countIntroducedBetween(from, to);
        int newBudget = Math.max(0, prefs.newPerDay() - introducedToday);

        List<Card> due = db.cards().dueCards(now, deckId, prefs.reviewPerDay());
        List<Card> fresh = newBudget > 0
                ? db.cards().newCards(deckId, newBudget)
                : new ArrayList<Card>();

        List<Card> queue = new ArrayList<>(due.size() + fresh.size());
        int i = 0;
        int j = 0;
        while (i < due.size() || j < fresh.size()) {
            for (int k = 0; k < 3 && i < due.size(); k++) {
                queue.add(due.get(i++));
            }
            if (j < fresh.size()) {
                queue.add(fresh.get(j++));
            }
        }
        if (prefs.shuffle()) {
            Collections.shuffle(queue);
        }
        return queue;
    }

    /** 记录一次评分，同时写复习日志。 */
    public Scheduler.Outcome grade(Card card, int grade) {
        long now = System.currentTimeMillis();
        int before = card.stage;
        Scheduler.Outcome outcome = Scheduler.apply(card.stage, grade, now, intervals());

        card.stage = outcome.stage;
        card.dueAt = outcome.dueAt;
        card.reviews = card.reviews + 1;
        card.lastReviewAt = now;
        if (card.firstReviewAt == 0) {
            card.firstReviewAt = now;
        }
        if (grade == Scheduler.GRADE_FORGOT) {
            card.lapses = card.lapses + 1;
        }
        db.cards().update(card);
        db.logs().insert(new ReviewLog(card.id, card.deckId, grade, now, before));
        return outcome;
    }

    /** 今天还需要复习多少张旧卡。 */
    public int dueNow(long deckId) {
        return db.cards().countDue(deckId);
    }

    /** 今天还剩多少张新卡额度。 */
    public int newLeftToday() {
        long now = System.currentTimeMillis();
        int introduced = db.cards().countIntroducedBetween(
                DateUtil.startOfDay(now), DateUtil.startOfNextDay(now));
        return Math.max(0, prefs.newPerDay() - introduced);
    }

    // ---------------------------------------------------------------- 统计

    public int todayReviewed() {
        long now = System.currentTimeMillis();
        return db.logs().countBetween(DateUtil.startOfDay(now), DateUtil.startOfNextDay(now));
    }

    public int totalCards() {
        return db.cards().countAll();
    }

    public int masteredCards() {
        return db.cards().countMastered(intervals().length);
    }

    public int learningCards() {
        return db.cards().countLearning(intervals().length);
    }

    public int newCards() {
        return db.cards().countNew();
    }

    /** 连续打卡天数（今天没复习则从昨天开始算）。 */
    public int streak() {
        Set<Long> days = new HashSet<>();
        for (Long t : db.logs().recentTimes()) {
            if (t != null) {
                days.add(DateUtil.startOfDay(t));
            }
        }
        long day = DateUtil.startOfDay(System.currentTimeMillis());
        if (!days.contains(day)) {
            day = DateUtil.addDays(day, -1);
        }
        int n = 0;
        while (days.contains(day) && n < 3650) {
            n++;
            day = DateUtil.addDays(day, -1);
        }
        return n;
    }

    /** 最近 7 天每天的复习量，index 0 是 6 天前，index 6 是今天。 */
    public int[] last7Days() {
        int[] out = new int[7];
        long today = DateUtil.startOfDay(System.currentTimeMillis());
        long from = DateUtil.addDays(today, -6);
        long to = DateUtil.startOfNextDay(today);
        List<ReviewLog> logs = db.logs().since(from);
        for (ReviewLog l : logs) {
            if (l.at >= to) {
                continue;
            }
            int idx = (int) Math.round((DateUtil.startOfDay(l.at) - from) / 86400000.0);
            if (idx >= 0 && idx < 7) {
                out[idx]++;
            }
        }
        return out;
    }

    public void clearAll() {
        db.cards().clear();
        db.logs().clear();
        db.decks().clear();
    }
}
