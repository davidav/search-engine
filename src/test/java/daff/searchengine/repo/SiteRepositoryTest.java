package daff.searchengine.repo;


import daff.searchengine.models.SiteEntity;
import daff.searchengine.models.StatusType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SiteRepositoryTest {
    @Autowired
    private SiteRepository siteRepository;
    private SiteEntity actualSite;

    @AfterEach
    void tearDown() {
        siteRepository.delete(actualSite);
    }
    @Test
    void findByUrl() {
        actualSite = insertIntoDatabaseSite("https://www.google.com/");

        Optional<SiteEntity> expectedSite = siteRepository.findByUrl("https://www.google.com/");

        assertTrue(expectedSite.isPresent());
        assertEquals("https://www.google.com/", expectedSite.get().getUrl());
    }

    @Test
    void getCountNoIndexedSites() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/", StatusType.FAILED);

        int count = siteRepository.getCountNoIndexedSites();

        assertEquals(1, count);
    }

    @NotNull
    private  SiteEntity insertIntoDatabaseSite(String url) {
        return siteRepository.save(SiteEntity.builder()
                .name("Site")
                .url(url)
                .status(StatusType.INDEXING)
                .statusTime(LocalDateTime.of(2023, Month.JANUARY, 10, 11, 12, 13))
                .lastError("Error!")
                .build());
        }

    @NotNull
    private  SiteEntity insertIntoDatabaseSite(String url, StatusType statusType) {
        return siteRepository.save(SiteEntity.builder()
                .name("Site")
                .url(url)
                .status(statusType)
                .statusTime(LocalDateTime.of(2023, Month.JANUARY, 10, 11, 12, 13))
                .lastError("Error!")
                .build());
    }
}
