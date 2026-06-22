package com.aihealthcoach.exercise.controller;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordUpdateRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseActivityOptionResponse;
import com.aihealthcoach.exercise.service.ExerciseService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/exercise")
@RequiredArgsConstructor
@Validated
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/physical-activities")
    public ResponseEntity<List<ExerciseActivityOptionResponse>> findExerciseActivityOptions(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(exerciseService.findExerciseActivityOptions(keyword));
    }

    @PostMapping("/records")
    public ResponseEntity<ExerciseRecordResponse> insertExerciseRecord(
            @Valid @RequestBody ExerciseRecordRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.insertExerciseRecord(userId, request));
    }

    @PutMapping("/records/{recordId}")
    public ResponseEntity<ExerciseRecordResponse> updateExerciseRecord(
            @PathVariable Long recordId,
            @Valid @RequestBody ExerciseRecordUpdateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.updateExerciseRecord(userId, recordId, request));
    }

    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteExerciseRecord(
            @PathVariable Long recordId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        exerciseService.deleteExerciseRecord(userId, recordId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/records")
    public ResponseEntity<List<ExerciseRecordResponse>> findExerciseRecordsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.findExerciseRecordsByDate(userId, date));
    }

    @GetMapping("/records/calendar")
    public ResponseEntity<List<LocalDate>> findExerciseDatesInMonth(
            @RequestParam int year,
            @RequestParam
            @Min(value = 1, message = "월은 1 이상이어야 합니다.")
            @Max(value = 12, message = "월은 12 이하이어야 합니다.")
            int month,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.findExerciseDatesInMonth(userId, year, month));
    }
}
