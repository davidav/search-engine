package daff.searchengine.repo;

import daff.searchengine.models.LemmaEntity;
import daff.searchengine.models.SiteEntity;
import daff.searchengine.models.StatusType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LemmaRepositoryTest {
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    private SiteEntity actualSite;


    @AfterEach
    void tearDown() {
        lemmaRepository.deleteAllBySiteId(actualSite.getId());
        siteRepository.delete(actualSite);
    }


    @Test
    void findAllByIdIn() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);

        List<LemmaEntity> expectedLemmas = lemmaRepository.findAllByIdIn(
                List.of(actualLemma1.getId(), actualLemma2.getId(), actualLemma3.getId()));

        assertEquals(3, expectedLemmas.size());
        assertEquals("страница", expectedLemmas.get(0).getLemma());
        assertEquals("первый", expectedLemmas.get(1).getLemma());
        assertEquals("материал", expectedLemmas.get(2).getLemma());

    }

    @Test
    void findAllByLemmaInAndSiteId() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        LemmaEntity actualLemma1 = insertIntoDatabaseLemma("страница", actualSite);
        LemmaEntity actualLemma2 = insertIntoDatabaseLemma("первый", actualSite);
        LemmaEntity actualLemma3 = insertIntoDatabaseLemma("материал", actualSite);

        List<LemmaEntity> expectedLemmas = lemmaRepository.findAllByLemmaInAndSiteId(
                Set.of(actualLemma1.getLemma(), actualLemma2.getLemma(), actualLemma3.getLemma()),
                actualSite.getId());

        assertEquals(3, expectedLemmas.size());
        assertEquals(actualSite.getId(), expectedLemmas.get(0).getSite().getId());
        assertEquals(actualSite.getId(), expectedLemmas.get(1).getSite().getId());
        assertEquals(actualSite.getId(), expectedLemmas.get(2).getSite().getId());
    }

    @Test
    void deleteAllBySiteId() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        insertIntoDatabaseLemma("страница", actualSite);
        insertIntoDatabaseLemma("первый", actualSite);
        insertIntoDatabaseLemma("материал", actualSite);

        lemmaRepository.deleteAllBySiteId(actualSite.getId());

        assertEquals(0, lemmaRepository.findAllByIdIn(List.of(actualSite.getId())).size());

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
    @NotNull
    private  LemmaEntity insertIntoDatabaseLemma(String lemma, SiteEntity site) {
        return lemmaRepository.save(LemmaEntity.builder()
                .lemma(lemma)
                .frequency(1)
                .site(site)
                .build());
    }
}