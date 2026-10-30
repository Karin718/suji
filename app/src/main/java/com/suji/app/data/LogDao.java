package com.suji.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogDao {

    @Insert
    void insert(ReviewLog log);

    @Insert
    void insertAll(List<ReviewLog> logs);

    @Query("SELECT * FROM review_logs WHERE at >= :from ORDER BY at ASC")
    List<ReviewLog> since(long from);

    @Query("SELECT COUNT(*) FROM review_logs WHERE at >= :from AND at < :to")
    int countBetween(long from, long to);

    @Query("SELECT COUNT(*) FROM review_logs")
    int count();

    /** 有复习记录的日期（毫秒时间戳，任意时刻），用于计算连续打卡。 */
    @Query("SELECT DISTINCT at FROM review_logs ORDER BY at DESC LIMIT 400")
    List<Long> recentTimes();

    @Query("DELETE FROM review_logs")
    void clear();
}
