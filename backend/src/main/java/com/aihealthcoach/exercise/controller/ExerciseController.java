package com.aihealthcoach.exercise.controller;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.PhysicalActivityResponse;
import com.aihealthcoach.exercise.service.ExerciseService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/physical-activities")
    public ResponseEntity<List<PhysicalActivityResponse>> findPhysicalActivities(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(exerciseService.findPhysicalActivities(keyword));
    }

    @PostMapping("/records")
    public ResponseEntity<ExerciseRecordResponse> insertExerciseRecord(
            @Valid @RequestBody ExerciseRecordRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.insertExerciseRecord(userId, request));
    }

    @GetMapping("/records")
    public ResponseEntity<List<ExerciseRecordResponse>> findExerciseRecordsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.findExerciseRecordsByDate(userId, date));
    }

    @GetMapping("/records/calendar")
    public ResponseEntity<List<LocalDate>> findExerciseDatesInMonth(@RequestParam int year, @RequestParam int month,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.findExerciseDatesInMonth(userId, year, month));
    }
}
