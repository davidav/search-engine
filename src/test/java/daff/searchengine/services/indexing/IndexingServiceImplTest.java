package daff.searchengine.services.indexing;

import daff.searchengine.config.SiteConfig;
import daff.searchengine.config.SitesConfig;
import daff.searchengine.dto.ResultDTO;
import daff.searchengine.exceptions.AppHelperException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {

    @InjectMocks
    private IndexingServiceImpl indexingService;
    @Mock
    private SitesConfig sitesConfig;


    @Test
    void startIndexing() {
        List<SiteConfig> sites =  new ArrayList<>(
                List.of(new SiteConfig("https://siteName/", "name")));
        when(sitesConfig.getSites()).thenReturn(sites);

        ResultDTO expectedResult = indexingService.startIndexing();

        assertFalse(indexingService.isStop());
        assertTrue(expectedResult.isResult());
    }

    @Test
    void indexingAlreadyStartedException() {

        IndexingServiceImpl indexingService = Mockito.spy(this.indexingService);

        when(indexingService.isIndexing()).thenReturn(true);

        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, indexingService::startIndexing);

        assertEquals("Индексация уже запущена", appHelperException.getMessage());

    }


    @Test
    void stopIndexing() {

        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, () -> indexingService.stopIndexing());

        assertEquals("Индексация не запущена", appHelperException.getMessage());
    }

    @Test
    void indexPage () {
        List<SiteConfig> sites = new ArrayList<>(
                List.of(new SiteConfig("https://slenta.ru/", "slenta")));

        when(sitesConfig.getSites()).thenReturn(sites);
        ResultDTO result = indexingService.indexPage("https://slenta.ru/");

        assertTrue(result.isResult());
    }

    @Test
    void indexPageUrlException () {

        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, () -> indexingService.indexPage(""));

        assertEquals("url error no protocol: ", appHelperException.getMessage());
    }

    @Test
    void indexPageThisPageIsOutsideException () {

        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, () -> indexingService.indexPage("https://lenta.ru/page"));

        assertEquals("Данная страница находится за пределами сайтов, указанных в конфигурационном файле",
                appHelperException.getMessage());
    }

    @Test
    void indexPageNotPageException () {
        List<SiteConfig> sites = new ArrayList<>(
                List.of(new SiteConfig("https://slenta.ru/", "slenta")));

        when(sitesConfig.getSites()).thenReturn(sites);
        AppHelperException appHelperException = assertThrows(
                AppHelperException.class, () -> indexingService.indexPage("https://slenta.ru/page.doc"));

        assertEquals("this url is not a page", appHelperException.getMessage());
    }


}