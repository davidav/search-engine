package daff.searchengine.services.indexing;

import daff.searchengine.config.SiteConfig;
import daff.searchengine.config.SitesConfig;
import daff.searchengine.dto.ResultDTO;
import daff.searchengine.exceptions.AppHelperException;
import daff.searchengine.models.SiteEntity;
import daff.searchengine.models.StatusType;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.repo.SiteRepository;
import daff.searchengine.util.LemmaFinder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;


@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    public static boolean isStop;
    private final SitesConfig sitesConfig;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;
    private ExecutorService executorService = null;
    private final List<Future<Boolean>> futures = new ArrayList<>();

    @Autowired
    public IndexingServiceImpl(SitesConfig sitesConfig,
                               SiteRepository siteRepository,
                               PageRepository pageRepository,
                               LemmaRepository lemmaRepository,
                               IndexRepository indexRepository,
                               LemmaFinder lemmaFinder) {
        this.sitesConfig = sitesConfig;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
    }

    static boolean isStop() {
        return isStop;
    }

    private static void setIsStop(boolean b) {
        isStop = b;
    }

    @Override
    public boolean isIndexing() {
        for (Future<Boolean> f : futures) {
            if (!f.isDone()) return true;
        }
        return false;
    }

    @Override
    public ResultDTO startIndexing() {
        if (isIndexing()) throw new AppHelperException("Индексация уже запущена");
        setIsStop(false);
        List<SiteEntity> sites = initSites();
        List<SiteAnalyzer> siteAnalyzers = new ArrayList<>();
        sites.forEach(site -> siteAnalyzers.add(
                new SiteAnalyzer(
                        site,
                        siteRepository,
                        pageRepository,
                        lemmaRepository,
                        indexRepository,
                        lemmaFinder)));
        executorService = Executors.newFixedThreadPool(siteAnalyzers.size());
        for (SiteAnalyzer siteAnalyzer : siteAnalyzers) {
            try {
                Future<Boolean> f = executorService.submit(siteAnalyzer, true);
                futures.add(f);
            } catch (NullPointerException | RejectedExecutionException e) {
                throw new AppHelperException("IndexingService error " + e.getMessage());
            }
        }
        return new ResultDTO(true);
    }

    @Override
    public ResultDTO stopIndexing() {
        if (!isIndexing())
            throw new AppHelperException("Индексация не запущена");
        setIsStop(true);
        futures.forEach(f -> f.cancel(true));
        if (executorService != null) executorService.shutdown();
        return new ResultDTO(true);
    }

    @Override
    public ResultDTO indexPage(String urlPage) {
        try {
            URL url = new URL(urlPage);
            if (sitesConfig.getSites().stream().noneMatch(s -> url.toString().startsWith(s.getUrl())))
                throw new AppHelperException("Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
            validateUrl(urlPage);
            new Thread(new OnePageAnalyzer(
                    url,
                    sitesConfig,
                    lemmaFinder,
                    siteRepository,
                    pageRepository,
                    lemmaRepository,
                    indexRepository))
                    .start();
        } catch (MalformedURLException e) {
            throw new AppHelperException("url error " + e.getMessage());
        }
        return new ResultDTO(true);
    }

    @NotNull
    private List<SiteEntity> initSites() {
        List<SiteEntity> sites = new ArrayList<>();
        for (SiteConfig siteConfig : sitesConfig.getSites()) {
            SiteEntity site = SiteEntity.builder()
                    .name(siteConfig.getName())
                    .url(siteConfig.getUrl())
                    .status(StatusType.INDEXING)
                    .statusTime(LocalDateTime.now())
                    .lastError("")
                    .agent(sitesConfig.getAgent())
                    .referer(sitesConfig.getReferer())
                    .build();
            sites.add(site);
        }
        return sites;
    }

    private void validateUrl(@NotNull String urlPage) {
        String urlPageLowerCase = urlPage.toLowerCase(Locale.ROOT);
        if (urlPageLowerCase.endsWith(".pdf") ||
                urlPageLowerCase.endsWith(".webp") ||
                urlPageLowerCase.endsWith(".jpg") ||
                urlPageLowerCase.endsWith(".jpeg") ||
                urlPageLowerCase.endsWith(".png") ||
                urlPageLowerCase.endsWith(".bmp") ||
                urlPageLowerCase.endsWith(".gif") ||
                urlPageLowerCase.endsWith(".mp3") ||
                urlPageLowerCase.endsWith(".mp4") ||
                urlPageLowerCase.endsWith(".avi") ||
                urlPageLowerCase.endsWith(".doc") ||
                urlPageLowerCase.endsWith(".docx")) {
            throw new AppHelperException("this url is not a page");
        }
    }
}