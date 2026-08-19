package com.ecommerce.media.seeder;

import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReelDataSeeder implements CommandLineRunner {

    private final ReelRepository reelRepository;
    private final ReelCommentRepository commentRepository;

    @Override
    public void run(String... args) {
        if (reelRepository.count() == 0) {
            log.info("Reels veritabanı boş, örnek dikey reels videoları yükleniyor...");

            // Örnek dikey test videoları ve kapak görselleri
            List<Reel> sampleReels = List.of(
                    Reel.builder()
                            .title("Oversize Yaz Kombini")
                            .description("%100 pamuklu oversize tişört stoklarda! #yazmodasi #kombin")
                            .videoUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                            .thumbnailUrl("https://images.pexels.com/photos/1036623/pexels-photo-1036623.jpeg?auto=compress&cs=tinysrgb&w=600")
                            .durationInSeconds(15)
                            .productId("prod_101")
                            .sellerId("seller_uuid_1")
                            .likeCount(1420L)
                            .viewCount(28500L)
                            .build(),
                    Reel.builder()
                            .title("Deri Ceket Şıklığı")
                            .description("Sonbahar sezonunun vazgeçilmez hakiki deri ceketi.")
                            .videoUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
                            .thumbnailUrl("https://images.pexels.com/photos/1126993/pexels-photo-1126993.jpeg?auto=compress&cs=tinysrgb&w=600")
                            .durationInSeconds(12)
                            .productId("prod_102")
                            .sellerId("seller_uuid_2")
                            .likeCount(850L)
                            .viewCount(14200L)
                            .build()
            );

            List<Reel> savedReels = reelRepository.saveAll(sampleReels);

            // Mavi tikli ve satıcı sabitli örnek yorumlar
            if (!savedReels.isEmpty()) {
                Reel firstReel = savedReels.get(0);
                List<ReelComment> sampleComments = List.of(
                        ReelComment.builder()
                                .reelId(firstReel.getId())
                                .userId("user_uuid_1")
                                .username("berk_dev")
                                .content("Kumaş kalitesi harika, kendi bedeninizi rahatlıkla alabilirsiniz.")
                                .isVerifiedBuyer(true)
                                .isPinned(false)
                                .likeCount(35L)
                                .build(),
                        ReelComment.builder()
                                .reelId(firstReel.getId())
                                .userId("seller_uuid_1")
                                .username("magaza_official")
                                .content("Kargo aynı gün çıkarılmaktadır, keyifli alışverişler dileriz!")
                                .isVerifiedBuyer(false)
                                .isPinned(true)
                                .likeCount(120L)
                                .build()
                );
                commentRepository.saveAll(sampleComments);
            }

            log.info("Örnek Reels ve yorum verileri başarıyla yüklendi.");
        }
    }
}