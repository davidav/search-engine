package daff.searchengine.repo;

import daff.searchengine.models.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
@SpringBootTest
class IndexRepositoryTest {
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    private SiteEntity actualSite;
    private final List<IndexEntity> actualIndexes = new ArrayList<>();

    @AfterEach
    void tearDown() {
        indexRepository.deleteAll(actualIndexes);
        lemmaRepository.deleteAllBySiteId(actualSite.getId());
        pageRepository.deleteAllBySiteId(actualSite.getId());
        siteRepository.delete(actualSite);
    }

    @Test
    void getSumRank() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        PageEntity actualPage = insertIntoDatabasePage("https://www.google.com/1", actualSite);
        PageEntity actualPage2 = insertIntoDatabasePage("https://www.google.ru/2", actualSite);
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma1, 3));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma2, 10));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma2, 5));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma3, 5));

        Integer expectedRank = indexRepository.getSumRank();

        assertEquals(23, expectedRank);
    }

    @Test
    void getSumRankBySite() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        PageEntity actualPage = insertIntoDatabasePage("https://www.google.com/1", actualSite);
        PageEntity actualPage2 = insertIntoDatabasePage("https://www.google.ru/2", actualSite);
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma1, 3));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma2, 10));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma2, 5));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma3, 5));

        Integer expectedRank = indexRepository.getSumRankBySite(actualSite.getId());

        assertEquals(23, expectedRank);

    }

    @Test
    void findAllByPageId() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        PageEntity actualPage = insertIntoDatabasePage("https://www.google.com/1", actualSite);
        PageEntity actualPage2 = insertIntoDatabasePage("https://www.google.ru/2", actualSite);
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma1, 3));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma2, 10));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma2, 5));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma3, 5));

        List<IndexEntity> expectedIndexes = indexRepository.findAllByPageId(actualPage.getId());

        assertEquals(2, expectedIndexes.size());
        assertEquals(actualPage.getId(), expectedIndexes.get(0).getPageId());
        assertEquals(actualPage.getId(), expectedIndexes.get(1).getPageId());

    }

    @Test
    void deleteAllByPageId() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        PageEntity actualPage = insertIntoDatabasePage("https://www.google.com/1", actualSite);
        PageEntity actualPage2 = insertIntoDatabasePage("https://www.google.ru/2", actualSite);
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma1, 3));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma2, 10));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma2, 5));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma3, 5));

        indexRepository.deleteAllByPageId(actualPage.getId());

        assertEquals(0, indexRepository.findAllByPageId(actualPage.getId()).size());

    }

    @Test
    void findAllByLemmaIdIn() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        PageEntity actualPage = insertIntoDatabasePage("https://www.google.com/1", actualSite);
        PageEntity actualPage2 = insertIntoDatabasePage("https://www.google.ru/2", actualSite);
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma1, 3));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage, actualLemma2, 10));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma2, 101));
        actualIndexes.add(insertIntoDatabaseIndex(actualPage2, actualLemma3, 5));

        List<IndexEntity> expectedIndexes = indexRepository.findAllByLemmaIdIn(List.of(actualLemma1.getId(), actualLemma2.getId()));

        assertEquals(2, expectedIndexes.size());
        assertEquals(actualLemma1.getId(), expectedIndexes.get(0).getLemmaId());
        assertEquals(actualLemma2.getId(), expectedIndexes.get(1).getLemmaId());
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