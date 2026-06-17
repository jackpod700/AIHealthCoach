package com.aihealthcoach.weight.service;

import com.aihealthcoach.user.mapper.UserMapper;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordRequest;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;
import com.aihealthcoach.weight.entity.WeightRecord;
import com.aihealthcoach.weight.exception.WeightRecordException;
import com.aihealthcoach.weight.mapper.WeightRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeightRecordServiceImpl implements WeightRecordService {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("500.00");

    private final WeightRecordMapper weightRecordMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public WeightRecordResponse upsertWeightRecord(Long userId, WeightRecordRequest request) {
        validateWeight(request.weightKg());

        WeightRecord savedRecord = weightRecordMapper.upsertWeightRecord(WeightRecord.builder()
                .userId(userId)
                .recordDate(request.recordDate())
                .weightKg(request.weightKg())
                .build());
        syncCurrentWeight(userId);

        return WeightRecordResponse.fromEntity(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightRecordResponse> findWeightRecords(Long userId, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return weightRecordMapper.findAllWeightRecords(userId)
                    .stream()
                    .map(WeightRecordResponse::fromEntity)
                    .toList();
        }

        validateDateRange(from, to);

        return weightRecordMapper.findWeightRecordsBetween(userId, from, to)
                .stream()
                .map(WeightRecordResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void deleteWeightRecord(Long userId, LocalDate recordDate) {
        int deletedCount = weightRecordMapper.deleteWeightRecordByDate(userId, recordDate);

        if (deletedCount == 0) {
            throw WeightRecordException.weightRecordNotFound();
        }

        syncCurrentWeight(userId);
    }

    private void syncCurrentWeight(Long userId) {
        WeightRecord latestRecord = weightRecordMapper.findLatestWeightRecord(userId);

        userMapper.updateUserProfileCurrentWeight(
                userId,
                latestRecord == null ? null : latestRecord.getWeightKg()
        );
    }

    private void validateWeight(BigDecimal weightKg) {
        if (weightKg == null
                || weightKg.compareTo(BigDecimal.ZERO) <= 0
                || weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw WeightRecordException.invalidWeight();
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw WeightRecordException.invalidDateRange();
        }
    }
}
