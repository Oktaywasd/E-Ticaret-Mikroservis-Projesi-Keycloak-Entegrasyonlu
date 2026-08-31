package com.ecommerce.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReelCacheSchedulerService {

    private final ReelService reelService;

    // 30 dakikada bir feed ilk sayfasını ısıtır
    @Scheduled(cron = "0 */30 * * * *")
    public void refreshReelsFeedCache() {
        log.info("Scheduled task başlatıldı: Reels Feed (Sayfa 0) önbelleği tazeleniyor...");
        reelService.clearReelsFeedCache();
        reelService.getReelsFeed(PageRequest.of(0, 10));
    }
}