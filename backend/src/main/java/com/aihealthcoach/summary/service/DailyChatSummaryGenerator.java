package com.aihealthcoach.summary.service;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummarySourceData;

public interface DailyChatSummaryGenerator {
    String generate(DailyChatSummarySourceData sourceData);
}
