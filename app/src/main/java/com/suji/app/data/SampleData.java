package com.suji.app.data;

import java.util.ArrayList;
import java.util.List;

/** 首次启动时的示例内容，让用户一进来就能体验完整的复习流程。 */
public final class SampleData {

    private SampleData() {
    }

    public static void seed(AppDatabase db) {
        long now = System.currentTimeMillis();

        Deck english = new Deck("示例 · 英语高频词");
        english.description = "10 个托福 / GRE 级别的形容词";
        english.createdAt = now;
        english.id = db.decks().insert(english);

        Deck poetry = new Deck("示例 · 古诗文");
        poetry.description = "中小学必背篇目，注意上下句衔接";
        poetry.createdAt = now + 1;
        poetry.id = db.decks().insert(poetry);

        Deck concept = new Deck("示例 · 概念速记");
        concept.description = "关于记忆方法本身的概念，边学边用";
        concept.createdAt = now + 2;
        concept.id = db.decks().insert(concept);

        List<Card> cards = new ArrayList<>();

        add(cards, english.id,
                "abandon",
                "v. 放弃；抛弃；遗弃",
                "a + band(乐队) + on：乐队解散了，只好放弃梦想",
                "单词");
        add(cards, english.id,
                "benevolent",
                "adj. 仁慈的；乐善好施的",
                "bene(好) + vol(意愿)：怀着好的意愿 → 仁慈的",
                "单词");
        add(cards, english.id,
                "candid",
                "adj. 坦率的；直言不讳的",
                "cand(白) + id：心里亮白 → 坦率",
                "单词");
        add(cards, english.id,
                "diligent",
                "adj. 勤奋的；用功的",
                "dilig(重视) + ent：把事当回事 → 勤奋",
                "单词");
        add(cards, english.id,
                "eloquent",
                "adj. 雄辩的；有说服力的",
                "e + loqu(说) + ent：说得出来 → 雄辩的",
                "单词");
        add(cards, english.id,
                "frugal",
                "adj. 节俭的；简朴的",
                "与 fruit 同源，本义「结出果实」→ 靠劳动所得，懂得节省",
                "单词");
        add(cards, english.id,
                "meticulous",
                "adj. 一丝不苟的；极细致的",
                "metic(害怕) + ulous：怕出错 → 一丝不苟",
                "单词");
        add(cards, english.id,
                "resilient",
                "adj. 有韧性的；能迅速恢复的",
                "re(回) + sil(跳)：弹回来 → 有韧性",
                "单词");
        add(cards, english.id,
                "tenacious",
                "adj. 顽强的；坚持不懈的",
                "ten(握) + acious：抓住不放 → 顽强的",
                "单词");
        add(cards, english.id,
                "versatile",
                "adj. 多才多艺的；用途广泛的",
                "vers(转) + atile：能转很多方向 → 多才多艺",
                "单词");

        add(cards, poetry.id,
                "《静夜思》下一句：举头望明月",
                "低头思故乡",
                "床前明月光，疑是地上霜。举头望明月，低头思故乡。—— 李白",
                "唐诗");
        add(cards, poetry.id,
                "《登鹳雀楼》后两句",
                "欲穷千里目，更上一层楼。",
                "白日依山尽，黄河入海流。欲穷千里目，更上一层楼。—— 王之涣",
                "唐诗");
        add(cards, poetry.id,
                "《春晓》作者与全文",
                "孟浩然：春眠不觉晓，处处闻啼鸟。夜来风雨声，花落知多少。",
                "注意「夜来风雨声」与「花落知多少」的因果关系",
                "唐诗");
        add(cards, poetry.id,
                "《望庐山瀑布》后两句",
                "飞流直下三千尺，疑是银河落九天。",
                "日照香炉生紫烟，遥看瀑布挂前川。飞流直下三千尺，疑是银河落九天。—— 李白",
                "唐诗");
        add(cards, poetry.id,
                "《论语》：学而时习之，后两句",
                "不亦说乎？有朋自远方来，不亦乐乎？",
                "学而时习之，不亦说乎？有朋自远方来，不亦乐乎？人不知而不愠，不亦君子乎？",
                "文言");
        add(cards, poetry.id,
                "《劝学》：不积跬步，下一句",
                "无以至千里",
                "不积跬步，无以至千里；不积小流，无以成江海。—— 荀子",
                "文言");

        add(cards, concept.id,
                "艾宾浩斯遗忘曲线",
                "遗忘在学习后立即开始，先快后慢：20 分钟约忘掉 42%，1 天约忘掉 66%，6 天约忘掉 75%。所以复习要抢在遗忘发生之前。",
                "横轴时间、纵轴保持率，是一条快速下跌后趋于平缓的曲线",
                "记忆法");
        add(cards, concept.id,
                "间隔重复",
                "把复习安排在快要忘记的时刻，间隔逐次拉长（如 1 天、2 天、4 天、7 天……），用最少的复习次数换最长的保持时间。",
                "和集中突击（考前熬夜）正好相反",
                "记忆法");
        add(cards, concept.id,
                "主动回忆",
                "合上材料，自己先把答案想出来 / 写出来，再对照。这比反复阅读有效得多，也叫测验效应（testing effect）。",
                "速记的先想再看就是这个原理",
                "记忆法");
        add(cards, concept.id,
                "费曼技巧",
                "用最简单的话把概念讲给一个外行听；讲不下去、卡住的地方，就是你没真正理解的地方，回去补那一块。",
                "四步：选概念 → 讲给小白 → 找到卡点 → 简化重讲",
                "记忆法");

        db.cards().insertAll(cards);
    }

    private static void add(List<Card> out, long deckId, String front, String back, String note, String tags) {
        Card c = new Card(deckId, front, back);
        c.note = note;
        c.tags = tags;
        out.add(c);
    }
}
