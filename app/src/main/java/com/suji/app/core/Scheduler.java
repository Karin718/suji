package com.suji.app.core;

import com.suji.app.data.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * 艾宾浩斯遗忘曲线调度器。
 *
 * <p>曲线由一串"间隔分钟数"描述，例如默认值：
 * 5,30,720,1440,2880,5760,10080,21600,43200,86400,172800
 * 分别对应 5 分钟 → 30 分钟 → 12 小时 → 1 天 → 2 天 → 4 天 → 7 天 → 15 天 → 30 天 → 60 天 → 120 天。</p>
 *
 * <p>卡片有 stage 字段表示"当前处在第几个间隔"。答对则前进一阶，答"模糊"则原地重来（间隔减半），
 * 答"忘记"则回到第 0 阶。stage 达到间隔总数时视为已掌握，不再进入复习队列。</p>
 */
public final class Scheduler {

    public static final int GRADE_FORGOT = 0;
    public static final int GRADE_VAGUE = 1;
    public static final int GRADE_GOOD = 2;

    /** 已掌握（永不再次到期）。 */
    public static final long NEVER = Long.MAX_VALUE / 4L;

    private static final long[] FALLBACK = {
            5, 30, 720, 1440, 2880, 5760, 10080, 21600, 43200, 86400, 172800
    };

    private Scheduler() {
    }

    public static long[] parse(String spec) {
        String s = spec == null ? "" : spec.trim();
        List<Long> out = new ArrayList<>();
        for (String part : s.split("[,，;；\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                long v = Long.parseLong(part);
                if (v > 0 && v <= 525600L) {
                    out.add(v);
                }
            } catch (NumberFormatException ignored) {
                // 忽略非法项
            }
        }
        if (out.isEmpty()) {
            if (!s.isEmpty() && !s.equals(Prefs.STD_INTERVALS)) {
                return parse(Prefs.STD_INTERVALS);
            }
            return FALLBACK.clone();
        }
        long[] arr = new long[out.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = out.get(i);
        }
        return arr;
    }

    public static final class Outcome {
        public int stage;
        public long dueAt;
    }

    public static Outcome apply(int stage, int grade, long now, long[] intervals) {
        int max = intervals.length;
        int s = Math.max(0, Math.min(stage, max));
        Outcome o = new Outcome();

        if (grade == GRADE_GOOD) {
            int next = Math.min(s + 1, max);
            o.stage = next;
            o.dueAt = next >= max ? NEVER : now + intervals[next] * 60000L;
        } else if (grade == GRADE_VAGUE) {
            int stay = Math.min(s, max - 1);
            o.stage = stay;
            long minutes = Math.max(5L, intervals[stay] / 2L);
            o.dueAt = now + minutes * 60000L;
        } else {
            o.stage = 0;
            o.dueAt = now + intervals[0] * 60000L;
        }
        return o;
    }

    /** 把分钟数写成人话。 */
    public static String human(long minutes) {
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        if (minutes < 1440) {
            long h = minutes / 60;
            return h + " 小时";
        }
        long d = minutes / 1440;
        return d + " 天";
    }

    /** 卡片列表里的状态文案。 */
    public static String statusText(int stage, int reviews, long dueAt, long[] intervals, long now) {
        if (reviews == 0) {
            return "未学";
        }
        if (stage >= intervals.length) {
            return "已掌握";
        }
        if (dueAt <= now) {
            return "待复习";
        }
        return "第 " + (stage + 1) + " 阶 · " + human(intervals[stage]);
    }

    /** 本次评分后下次什么时候复习。 */
    public static String nextText(int stage, int grade, long[] intervals, long now) {
        Outcome o = apply(stage, grade, now, intervals);
        if (o.dueAt >= NEVER) {
            return "已掌握，不再提醒";
        }
        return human(Math.max(1L, (o.dueAt - now) / 60000L)) + "后";
    }
}
