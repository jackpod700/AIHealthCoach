package com.aihealthcoach.weight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aihealthcoach.user.mapper.UserMapper;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordRequest;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;
import com.aihealthcoach.weight.entity.WeightRecord;
import com.aihealthcoach.weight.exception.WeightRecordErrorCode;
import com.aihealthcoach.weight.exception.WeightRecordException;
import com.aihealthcoach.weight.mapper.WeightRecordMapper;
import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;
import com.aihealthcoach.summary.service.DailyChatSummaryStateService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeightRecordServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 6, 17);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-20T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private WeightRecordMapper weightRecordMapper;

    @Mock
    private UserMapper userMapper;
    @Mock
    private DailyChatSummaryStateService dailyChatSummaryStateService;

    private WeightRecordServiceImpl weightRecordService;

    @BeforeEach
    void setUp() {
        weightRecordService = new WeightRecordServiceImpl(
                weightRecordMapper,
                userMapper,
                CLOCK,
                dailyChatSummaryStateService
        );
    }

    @Test
    void upsertWeightRecordReturnsMinimalResponseAndSyncsCurrentWeight() {
        WeightRecord savedRecord = WeightRecord.builder()
                .userId(USER_ID)
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68.40"))
                .updatedAt(LocalDateTime.of(2026, 6, 17, 12, 0))
                .build();
        when(weightRecordMapper.upsertWeightRecord(any(WeightRecord.class))).thenReturn(savedRecord);
        when(weightRecordMapper.findLatestWeightRecord(USER_ID)).thenReturn(savedRecord);

        WeightRecordResponse response = weightRecordService.upsertWeightRecord(
                USER_ID,
                new WeightRecordRequest(RECORD_DATE, new BigDecimal("68.40"))
        );

        assertThat(response.recordDate()).isEqualTo(RECORD_DATE);
        assertThat(response.weightKg()).isEqualByComparingTo("68.40");
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 17, 12, 0));
        verify(userMapper).updateUserProfileCurrentWeight(USER_ID, new BigDecimal("68.40"));
        verify(dailyChatSummaryStateService).markChanged(USER_ID, RECORD_DATE, DailyChatSummaryChangeSource.WEIGHT);
    }

    @Test
    void upsertWeightRecordAllowsIntegerWeight() {
        WeightRecord savedRecord = WeightRecord.builder()
                .userId(USER_ID)
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68"))
                .build();
        when(weightRecordMapper.upsertWeightRecord(any(WeightRecord.class))).thenReturn(savedRecord);
        when(weightRecordMapper.findLatestWeightRecord(USER_ID)).thenReturn(savedRecord);

        weightRecordService.upsertWeightRecord(
                USER_ID,
                new WeightRecordRequest(RECORD_DATE, new BigDecimal("68"))
        );

        ArgumentCaptor<WeightRecord> captor = ArgumentCaptor.forClass(WeightRecord.class);
        verify(weightRecordMapper).upsertWeightRecord(captor.capture());
        assertThat(captor.getValue().getWeightKg()).isEqualByComparingTo("68");
    }

    @Test
    void upsertWeightRecordAllowsSingleDecimalWeight() {
        WeightRecord savedRecord = WeightRecord.builder()
                .userId(USER_ID)
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68.4"))
                .build();
        when(weightRecordMapper.upsertWeightRecord(any(WeightRecord.class))).thenReturn(savedRecord);
        when(weightRecordMapper.findLatestWeightRecord(USER_ID)).thenReturn(savedRecord);

        weightRecordService.upsertWeightRecord(
                USER_ID,
                new WeightRecordRequest(RECORD_DATE, new BigDecimal("68.4"))
        );

        ArgumentCaptor<WeightRecord> captor = ArgumentCaptor.forClass(WeightRecord.class);
        verify(weightRecordMapper).upsertWeightRecord(captor.capture());
        assertThat(captor.getValue().getWeightKg()).isEqualByComparingTo("68.4");
    }

    @Test
    void upsertWeightRecordPropagatesSummaryStateFailure() {
        WeightRecord savedRecord = WeightRecord.builder()
                .userId(USER_ID)
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68.40"))
                .build();
        when(weightRecordMapper.upsertWeightRecord(any(WeightRecord.class))).thenReturn(savedRecord);
        when(weightRecordMapper.findLatestWeightRecord(USER_ID)).thenReturn(savedRecord);
        org.mockito.Mockito.doThrow(new IllegalStateException("summary state unavailable"))
                .when(dailyChatSummaryStateService)
                .markChanged(USER_ID, RECORD_DATE, DailyChatSummaryChangeSource.WEIGHT);

        assertThatThrownBy(() -> weightRecordService.upsertWeightRecord(
                USER_ID,
                new WeightRecordRequest(RECORD_DATE, new BigDecimal("68.40"))
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void upsertWeightRecordAllowsToday() {
        LocalDate today = LocalDate.of(2026, 6, 20);
        WeightRecord savedRecord = WeightRecord.builder()
                .userId(USER_ID)
                .recordDate(today)
                .weightKg(new BigDecimal("68.40"))
                .updatedAt(LocalDateTime.of(2026, 6, 20, 12, 0))
                .build();
        when(weightRecordMapper.upsertWeightRecord(any(WeightRecord.class))).thenReturn(savedRecord);
        when(weightRecordMapper.findLatestWeightRecord(USER_ID)).thenReturn(savedRecord);

        WeightRecordResponse response = weightRecordService.upsertWeightRecord(
                USER_ID,
                new WeightRecordRequest(today, new BigDecimal("68.40"))
        );

        assertThat(response.recordDate()).isEqualTo(today);
        verify(weightRecordMapper).upsertWeightRecord(any(WeightRecord.class));
    }

    @Test
    void findWeightRecordsReturnsAllRecordsWhenRangeIsNotProvided() {
        when(weightRecordMapper.findAllWeightRecords(USER_ID)).thenReturn(List.of(WeightRecord.builder()
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68.40"))
                .updatedAt(LocalDateTime.of(2026, 6, 17, 12, 0))
                .build()));

        List<WeightRecordResponse> responses = weightRecordService.findWeightRecords(USER_ID, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).recordDate()).isEqualTo(RECORD_DATE);
        assertThat(responses.get(0).weightKg()).isEqualByComparingTo("68.40");
    }

    @Test
    void findWeightRecordsRejectsInvalidDateRange() {
        assertThatThrownBy(() -> weightRecordService.findWeightRecords(USER_ID, RECORD_DATE, RECORD_DATE.minusDays(1)))
                .isInstanceOf(WeightRecordException.class)
                .extracting("errorCode")
                .isEqualTo(WeightRecordErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void upsertWeightRecordRejectsFutureRecordDateBeforeWriting() {
        LocalDate futureDate = LocalDate.of(2026, 6, 21);

        assertThatThrownBy(() -> weightRecordService.upsertWeightRecord(
                USER_ID,
                new WeightRecordRequest(futureDate, new BigDecimal("68.40"))
        ))
                .isInstanceOf(WeightRecordException.class)
                .extracting("errorCode")
                .isEqualTo(WeightRecordErrorCode.FUTURE_RECORD_DATE);

        verify(weightRecordMapper, never()).upsertWeightRecord(any(WeightRecord.class));
        verify(userMapper, never()).updateUserProfileCurrentWeight(any(), any());
    }

    @Test
    void deleteWeightRecordSyncsCurrentWeightToLatestRecord() {
        WeightRecord targetRecord = WeightRecord.builder()
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68.40"))
                .build();
        WeightRecord latestRecord = WeightRecord.builder()
                .recordDate(RECORD_DATE.minusDays(1))
                .weightKg(new BigDecimal("68.10"))
                .build();
        when(weightRecordMapper.findWeightRecordByDate(USER_ID, RECORD_DATE)).thenReturn(targetRecord);
        when(weightRecordMapper.countWeightRecordsUpToTwo(USER_ID)).thenReturn(2);
        when(weightRecordMapper.deleteWeightRecordByDate(USER_ID, RECORD_DATE)).thenReturn(1);
        when(weightRecordMapper.findLatestWeightRecord(USER_ID)).thenReturn(latestRecord);

        weightRecordService.deleteWeightRecord(USER_ID, RECORD_DATE);

        verify(userMapper).updateUserProfileCurrentWeight(USER_ID, new BigDecimal("68.10"));
    }

    @Test
    void deleteWeightRecordRejectsMissingRecord() {
        when(weightRecordMapper.findWeightRecordByDate(USER_ID, RECORD_DATE)).thenReturn(null);

        assertThatThrownBy(() -> weightRecordService.deleteWeightRecord(USER_ID, RECORD_DATE))
                .isInstanceOf(WeightRecordException.class)
                .extracting("errorCode")
                .isEqualTo(WeightRecordErrorCode.WEIGHT_RECORD_NOT_FOUND);
        verify(weightRecordMapper, never()).deleteWeightRecordByDate(USER_ID, RECORD_DATE);
    }

    @Test
    void deleteWeightRecordRejectsLastRemainingRecord() {
        WeightRecord targetRecord = WeightRecord.builder()
                .recordDate(RECORD_DATE)
                .weightKg(new BigDecimal("68.40"))
                .build();
        when(weightRecordMapper.findWeightRecordByDate(USER_ID, RECORD_DATE)).thenReturn(targetRecord);
        when(weightRecordMapper.countWeightRecordsUpToTwo(USER_ID)).thenReturn(1);

        assertThatThrownBy(() -> weightRecordService.deleteWeightRecord(USER_ID, RECORD_DATE))
                .isInstanceOf(WeightRecordException.class)
                .extracting("errorCode")
                .isEqualTo(WeightRecordErrorCode.MINIMUM_WEIGHT_RECORD_REQUIRED);
        verify(weightRecordMapper, never()).deleteWeightRecordByDate(USER_ID, RECORD_DATE);
    }
}
