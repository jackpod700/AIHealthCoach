package com.aihealthcoach.common.fatsecret;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "FatSecret Test", description = "FatSecret API connection test")
@RestController
@RequestMapping("/api/fatsecret/test")
@RequiredArgsConstructor
public class FatSecretTestController {

    private final FatSecretTestService fatSecretTestService;

    @Operation(summary = "Test FatSecret food search connection")
    @GetMapping("/search")
    public ResponseEntity<FatSecretSearchTestResponse> search(
            @RequestParam(required = false, defaultValue = "apple") String query,
            @RequestParam(required = false, defaultValue = "5") Integer maxResults,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String language
    ) {
        return ResponseEntity.ok(fatSecretTestService.search(query, maxResults, region, language));
    }
}
