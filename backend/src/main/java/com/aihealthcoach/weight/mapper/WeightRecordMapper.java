package com.aihealthcoach.weight.mapper;

import com.aihealthcoach.weight.entity.WeightRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WeightRecordMapper {
    WeightRecord upsertWeightRecord(WeightRecord weightRecord);

    List<WeightRecord> findWeightRecordsBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<WeightRecord> findAllWeightRecords(@Param("userId") Long userId);

    WeightRecord findWeightRecordByDate(
            @Param("userId") Long userId,
            @Param("recordDate") LocalDate recordDate
    );

    WeightRecord findLatestWeightRecord(@Param("userId") Long userId);

    BigDecimal findLatestWeightOnOrBefore(
            @Param("userId") Long userId,
            @Param("recordDate") LocalDate recordDate
    );

    int countWeightRecordsUpToTwo(@Param("userId") Long userId);

    int deleteWeightRecordByDate(
            @Param("userId") Long userId,
            @Param("recordDate") LocalDate recordDate
    );
}
