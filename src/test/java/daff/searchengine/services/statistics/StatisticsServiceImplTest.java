package daff.searchengine.services.statistics;

import daff.searchengine.dto.statistics.StatisticsDTO;
import daff.searchengine.models.*;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.repo.SiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class StatisticsServiceImplTest {

    @Autowired
    private StatisticsServiceImpl statisticsService;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    private SiteEntity testSite;
    private final List<IndexEntity> testIndexes = new ArrayList<>();

    @AfterEach
    void tearDown() {
        indexRepository.deleteAll(testIndexes);
        lemmaRepository.deleteAllBySiteId(testSite.getId());
        pageRepository.deleteAllBySiteId(testSite.getId());
        siteRepository.delete(testSite);
    }

    @Test
    void getStatistics() {

        StatisticsDTO beforeStartStatisticsDTO = statisticsService.getStatistics();
        testSite = insertIntoDatabaseSite("https://www.test.com/");
        PageEntity testPage = insertIntoDatabasePage("https://www.test.com/1", testSite);
        PageEntity testPage2 = insertIntoDatabasePage("https://www.test.com/2", testSite);
        LemmaEntity testLemma1 = insertIntoDatabaseLemma("страница", testSite);
        LemmaEntity testLemma2 = insertIntoDatabaseLemma("первый", testSite);
        LemmaEntity testLemma3 = insertIntoDatabaseLemma("материал", testSite);
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma1, 3));
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma2, 10));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma2, 5));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma3, 5));

        StatisticsDTO expectedStatisticsDTO = statisticsService.getStatistics();

        assertTrue(expectedStatisticsDTO.isResult());
        assertEquals(beforeStartStatisticsDTO.getStatistics().getTotal().getSites() + 1,
                expectedStatisticsDTO.getStatistics().getTotal().getSites());
        assertEquals(beforeStartStatisticsDTO.getStatistics().getTotal().getPages() + 2,
                expectedStatisticsDTO.getStatistics().getTotal().getPages());
        assertEquals(beforeStartStatisticsDTO.getStatistics().getTotal().getLemmas() + 23,
                expectedStatisticsDTO.getStatistics().getTotal().getLemmas());
        assertEquals(beforeStartStatisticsDTO.getStatistics().getDetailed().size() + 1,
                expectedStatisticsDTO.getStatistics().getDetailed().size());
        assertTrue(expectedStatisticsDTO.getStatistics().getDetailed().stream()
                .anyMatch(detailed -> detailed.getUrl().equals("https://www.test.com/")));
        assertEquals(expectedStatisticsDTO.getStatistics().getDetailed().stream()
                .filter(detailed -> detailed.getUrl().equals("https://www.test.com/"))
                .findFirst().get().getPages(), 2);
        assertEquals(expectedStatisticsDTO.getStatistics().getDetailed().stream()
                .filter(detailed -> detailed.getUrl().equals("https://www.test.com/"))
                .findFirst().get().getLemmas(), 23);

    }

    @NotNull
    private  IndexEntity insertIntoDatabaseIndex(@NotNull PageEntity page, @NotNull LemmaEntity lemma, int rank) {
        return indexRepository.save(IndexEntity.builder()
                .pageId(page.getId())
                .lemmaId(lemma.getId())
                .rank(rank)
                .build());
    }

    @NotNull
    private  LemmaEntity insertIntoDatabaseLemma(String lemma, SiteEntity site) {
        return lemmaRepository.save(LemmaEntity.builder()
                .lemma(lemma)
                .frequency(1)
                .site(site)
                .build());
    }

    @NotNull
    private  PageEntity insertIntoDatabasePage(String urlPage, SiteEntity site) {
        return pageRepository.save(PageEntity.builder()
                .path(urlPage)
                .code(200)
                .content("content")
                .site(site)
                .build());
    }

    @NotNull
    private SiteEntity insertIntoDatabaseSite(String url) {
        return siteRepository.save(SiteEntity.builder()
                .name("Site")
                .url(url)
                .status(StatusType.INDEXING)
                .statusTime(LocalDateTime.of(2023, Month.JANUARY, 10, 11, 12, 13))
                .lastError("Error!")
                .build());
    }
}