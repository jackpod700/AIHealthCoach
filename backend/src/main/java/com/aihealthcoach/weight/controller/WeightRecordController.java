package com.aihealthcoach.weight.controller;

import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordDeleteRequest;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordRequest;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;
import com.aihealthcoach.weight.service.WeightRecordService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weight-records")
@RequiredArgsConstructor
public class WeightRecordController {

    private final WeightRecordService weightRecordService;

    @PutMapping
    public ResponseEntity<WeightRecordResponse> upsertWeightRecord(
            @Valid @RequestBody WeightRecordRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(weightRecordService.upsertWeightRecord(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<WeightRecordResponse>> findWeightRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(weightRecordService.findWeightRecords(userId, from, to));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteWeightRecord(
            @Valid @RequestBody WeightRecordDeleteRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        weightRecordService.deleteWeightRecord(userId, request.recordDate());
        return ResponseEntity.ok().build();
    }
}
