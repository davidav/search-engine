package daff.searchengine.services.search;

import daff.searchengine.dto.ResultDTO;
import daff.searchengine.exceptions.AppHelperException;
import daff.searchengine.models.*;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.repo.SiteRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SearchServiceImplTest {

    @Autowired
    private SearchServiceImpl searchService;
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

    @ParameterizedTest
    @CsvSource({"страница, 0, 10, https://www.test.com/"})
    void search(String query, int offset, int limit, String siteUrl) {
        testSite = insertIntoDatabaseSite("https://www.test.com/", "Site", StatusType.INDEXED);
        PageEntity testPage = insertIntoDatabasePage("/1", testSite, "это content, а не страница");
        PageEntity testPage2 = insertIntoDatabasePage("/2", testSite, "content");
        LemmaEntity testLemma1 = insertIntoDatabaseLemma("страница", testSite);
        LemmaEntity testLemma2 = insertIntoDatabaseLemma("первый", testSite);
        LemmaEntity testLemma3 = insertIntoDatabaseLemma("материал", testSite);
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma1, 3));
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma2, 10));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma2, 5));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma3, 5));

        ResultDTO expectedResult = searchService.search(query, offset, limit, siteUrl);

        assertTrue(expectedResult.isResult());
        assertEquals(1, expectedResult.getCount());
        assertEquals(1, expectedResult.getData().size());
        assertEquals("https://www.test.com/", expectedResult.getData().get(0).getSite());
        assertEquals("Site", expectedResult.getData().get(0).getSiteName());
        assertEquals("это content, а не <b>страница</b>... <br />", expectedResult.getData().get(0).getSnippet());

    }

    @ParameterizedTest
    @CsvSource({"страница, 0, 10, https://www.test.com/"})
    void emptyQueryException(String query, int offset, int limit, String siteUrl) {
        testSite = insertIntoDatabaseSite("https://www.test.com/");
        PageEntity testPage = insertIntoDatabasePage("/1", testSite);
        PageEntity testPage2 = insertIntoDatabasePage("/2", testSite);
        LemmaEntity testLemma1 = insertIntoDatabaseLemma("страница", testSite);
        LemmaEntity testLemma2 = insertIntoDatabaseLemma("первый", testSite);
        LemmaEntity testLemma3 = insertIntoDatabaseLemma("материал", testSite);
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma1, 3));
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma2, 10));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma2, 5));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma3, 5));

        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, () -> searchService.search("", offset, limit, siteUrl));

        assertEquals("Задан пустой поисковый запрос", appHelperException.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"страница, 0, 10, https://www.test.com/"})
    void notIndexingSiteException(String query, int offset, int limit, String siteUrl) {
        testSite = insertIntoDatabaseSite("https://www.test.com/", "Site", StatusType.FAILED);
        PageEntity testPage = insertIntoDatabasePage("/1", testSite);
        PageEntity testPage2 = insertIntoDatabasePage("/2", testSite);
        LemmaEntity testLemma1 = insertIntoDatabaseLemma("страница", testSite);
        LemmaEntity testLemma2 = insertIntoDatabaseLemma("первый", testSite);
        LemmaEntity testLemma3 = insertIntoDatabaseLemma("материал", testSite);
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma1, 3));
        testIndexes.add(insertIntoDatabaseIndex(testPage, testLemma2, 10));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma2, 5));
        testIndexes.add(insertIntoDatabaseIndex(testPage2, testLemma3, 5));

        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, () -> searchService.search(query, offset, limit, siteUrl));

        assertEquals("Заданый сайт не проиндесирован", appHelperException.getMessage());
    }

    @NotNull
    private SiteEntity insertIntoDatabaseSite(String url) {
        return siteRepository.save(SiteEntity.builder()
                .name("Site")
                .url(url)
                .status(StatusType.INDEXED)
                .statusTime(LocalDateTime.of(2023, Month.JANUARY, 10, 11, 12, 13))
                .lastError("Error!")
                .build());
    }
    @NotNull
    private SiteEntity insertIntoDatabaseSite(String url, String name, StatusType statusType) {
        return siteRepository.save(SiteEntity.builder()
                .name(name)
                .url(url)
                .status(statusType)
                .statusTime(LocalDateTime.of(2023, Month.JANUARY, 10, 11, 12, 13))
                .lastError("Error!")
                .build());
    }
    @NotNull
    private IndexEntity insertIntoDatabaseIndex(@NotNull PageEntity page, @NotNull LemmaEntity lemma, int rank) {
        return indexRepository.save(IndexEntity.builder()
                .pageId(page.getId())
                .lemmaId(lemma.getId())
                .rank(rank)
                .build());
    }
    @NotNull
    private LemmaEntity insertIntoDatabaseLemma(String lemma, SiteEntity site) {
        return lemmaRepository.save(LemmaEntity.builder()
                .lemma(lemma)
                .frequency(1)
                .site(site)
                .build());
    }
    @NotNull
    private PageEntity insertIntoDatabasePage(String urlPage, SiteEntity site) {
        return pageRepository.save(PageEntity.builder()
                .path(urlPage)
                .code(200)
                .content("content")
                .site(site)
                .build());
    }
    @NotNull
    private PageEntity insertIntoDatabasePage(String urlPage, SiteEntity site, String content) {
        return pageRepository.save(PageEntity.builder()
                .path(urlPage)
                .code(200)
                .content(content)
                .site(site)
                .build());
    }
}


