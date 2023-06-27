package daff.searchengine.services.indexing;

import daff.searchengine.exceptions.AppHelperException;
import daff.searchengine.models.SiteEntity;
import daff.searchengine.models.StatusType;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.repo.SiteRepository;
import daff.searchengine.util.LemmaFinder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Getter
@Setter
public class SiteAnalyzer implements Runnable {

    private SiteEntity site;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;
    private CopyOnWriteArrayList<String> siteLinks = new CopyOnWriteArrayList<>();
    private ForkJoinPool forkJoinPool;

    public SiteAnalyzer(SiteEntity site,
                        SiteRepository siteRepository,
                        PageRepository pageRepository,
                        LemmaRepository lemmaRepository,
                        IndexRepository indexRepository,
                        LemmaFinder lemmaFinder) {
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;

    }

    @Override
    public void run() {
        log.info("start analyze  {}", site.getName());
        Boolean isCompleted;
        cleaningDB(site);
        site = siteRepository.save(site);
        forkJoinPool = new ForkJoinPool();
        try {
            URL url = new URL(site.getUrl());
            isCompleted = forkJoinPool.invoke(
                    new PageAnalyzer(url, site, lemmaFinder, siteLinks,
                            pageRepository, lemmaRepository, indexRepository));
        } catch (NullPointerException | IllegalArgumentException | MalformedURLException e) {
            throw new AppHelperException("SiteAnalyzer error " + e.getMessage());
        }
        if (isCompleted) {
            site.setStatus(StatusType.INDEXED);
            site.setLastError("");
        } else {
            site.setStatus(StatusType.FAILED);
            site.setLastError("interrupted");
        }
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        log.info("end analyze {}", site.getName());
    }

    private void cleaningDB(@NotNull SiteEntity site) {
        if (siteRepository.findByUrl(site.getUrl()).isPresent()) {
            SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).get();
            pageRepository.deleteAllBySiteId(siteEntity.getId());
            siteRepository.delete(siteEntity);
        }
    }

}
