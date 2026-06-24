package com.aihealthcoach.memory.controller;

import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryDeleteResponse;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.service.UserMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-memories")
@RequiredArgsConstructor
public class UserMemoryController {

    private final UserMemoryService userMemoryService;

    @GetMapping
    @Operation(summary = "활성화된 사용자 메모리 전체 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<List<UserMemoryResponse>> findMemories(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userMemoryService.findMemoriesByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "사용자 메모리 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "메모리 생성 성공"),
            @ApiResponse(responseCode = "400", description = "content validation 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<UserMemoryResponse> createMemory(
            @Valid @RequestBody UserMemoryCreateRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(userMemoryService.createMemory(userId, request));
    }

    @DeleteMapping("/{memoryId}")
    @Operation(summary = "해당 id의 사용자 메모리 비활성화")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리 비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "메모리를 찾을 수 없거나 소유하지 않음")
    })
    public ResponseEntity<UserMemoryDeleteResponse> deactivateMemory(
            @PathVariable Long memoryId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        userMemoryService.deactivateMemory(userId, memoryId);
        return ResponseEntity.ok(new UserMemoryDeleteResponse(null));
    }
}
