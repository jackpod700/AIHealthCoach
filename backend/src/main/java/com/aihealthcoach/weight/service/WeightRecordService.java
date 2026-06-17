package com.aihealthcoach.weight.service;

import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordRequest;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;
import java.time.LocalDate;
import java.util.List;

public interface WeightRecordService {
    WeightRecordResponse upsertWeightRecord(Long userId, WeightRecordRequest request);

    List<WeightRecordResponse> findWeightRecords(Long userId, LocalDate from, LocalDate to);

    void deleteWeightRecord(Long userId, LocalDate recordDate);
}
