package com.suji.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 一次复习记录，用于统计与打卡。 */
@Entity(tableName = "review_logs", indices = {@Index("at"), @Index("cardId")})
public class ReviewLog {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;

    public long deckId;

    /** 0 = 忘记，1 = 模糊，2 = 记得。 */
    public int grade;

    /** 复习时间（毫秒时间戳）。 */
    public long at;

    /** 复习前的阶段。 */
    public int stageBefore;

    public ReviewLog() {
    }

    @Ignore
    public ReviewLog(long cardId, long deckId, int grade, long at, int stageBefore) {
        this.cardId = cardId;
        this.deckId = deckId;
        this.grade = grade;
        this.at = at;
        this.stageBefore = stageBefore;
    }
}
