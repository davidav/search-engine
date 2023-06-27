package daff.searchengine.repo;

import daff.searchengine.models.PageEntity;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class PageRepositoryTest {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    private SiteEntity actualSite;


    @AfterEach
    void tearDown() {
        pageRepository.deleteAllBySiteId(actualSite.getId());
        siteRepository.delete(actualSite);
    }

    @Test
    void getCountBySite() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        insertIntoDatabasePage("https://www.google.com/1", actualSite);
        insertIntoDatabasePage("https://www.google.com/2", actualSite);

        int count = pageRepository.getCountBySite(actualSite.getId());

        assertEquals(2, count);

    }

    @Test
    void deleteAllBySiteId() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        insertIntoDatabasePage("https://www.google.com/1", actualSite);
        insertIntoDatabasePage("https://www.google.com/2", actualSite);

        pageRepository.deleteAllBySiteId(actualSite.getId());

        assertEquals(0, pageRepository.getCountBySite(actualSite.getId()));

    }

    @Test
    void findByPathAndSite() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        insertIntoDatabasePage("https://www.google.com/page", actualSite);

        Optional<PageEntity> expectedPage = pageRepository.findByPathAndSite("https://www.google.com/page", actualSite);

        assertTrue(expectedPage.isPresent());
        assertEquals("https://www.google.com/page", expectedPage.get().getPath());
        assertEquals("https://www.google.com/", expectedPage.get().getSite().getUrl());
    }

    @Test
    void findByIdIn() {

        actualSite = insertIntoDatabaseSite("https://www.google.com/");
        PageEntity actualPage1 = insertIntoDatabasePage("https://www.google.com/1", actualSite);
        PageEntity actualPage2 = insertIntoDatabasePage("https://www.google.com/2", actualSite);

        List<PageEntity> expectedPages = pageRepository.findByIdIn(List.of(actualPage1.getId(), actualPage2.getId()));

        assertEquals(2,expectedPages.size());
        assertEquals("https://www.google.com/1", expectedPages.get(0).getPath());
        assertEquals("https://www.google.com/2", expectedPages.get(1).getPath());
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
    private  PageEntity insertIntoDatabasePage(String urlPage, SiteEntity site) {
        return pageRepository.save(PageEntity.builder()
                .path(urlPage)
                .code(200)
                .content("content")
                .site(site)
                .build());
    }
}