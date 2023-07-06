package daff.searchengine.services.search;


import daff.searchengine.dto.ResultDTO;
import daff.searchengine.dto.search.SearchRequest;
import daff.searchengine.dto.search.SearchResult;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    private final LemmaFinder lemmaFinder;
    private volatile CopyOnWriteArraySet<SearchResult> searchResults;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;


    @Autowired
    public SearchServiceImpl(LemmaFinder lemmaFinder,
                             SiteRepository siteRepository,
                             PageRepository pageRepository,
                             LemmaRepository lemmaRepository,
                             IndexRepository indexRepository) {
        this.lemmaFinder = lemmaFinder;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public ResultDTO search(String query, int offset, int limit, String siteUrl) {
        searchResults = new CopyOnWriteArraySet<>();
        Optional<SiteEntity> siteOptional = siteRepository.findByUrl(siteUrl);
        validateInputData(query, siteOptional);
        List<SiteEntity> sites = createListSites(siteOptional);
        List<SearchRequest> searchRequests = createListSearchRequest(query, offset, limit, sites);
        List<SearchSiteAnalyzer> searchSiteAnalyzers = new ArrayList<>();
        searchRequests.forEach(searchRequest -> searchSiteAnalyzers.add(
                new SearchSiteAnalyzer(  searchRequest, searchResults, lemmaFinder,
                        pageRepository, lemmaRepository, indexRepository)));
        ExecutorService executorService = Executors.newFixedThreadPool(sites.size());
        try {
            List<Future<Boolean>> futures = executorService.invokeAll(searchSiteAnalyzers);
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new AppHelperException("SearchService error! " + e.getMessage());
        }
        executorService.shutdown();
        int count = searchResults.size();
        List<SearchResult> dataAll = new ArrayList<>(searchResults);
        dataAll.sort(SearchResult::compareTo);
        List<SearchResult> data = dataAll.subList(offset, Math.min(dataAll.size(), offset + limit));
        log.info("end search");
        return new ResultDTO(true, count, data);
    }

    private void validateInputData(@NotNull String query, Optional<SiteEntity> site) {
        if (query.equals(""))
            throw new AppHelperException("Задан пустой поисковый запрос");
        if (site.isEmpty()) {
            if (isSiteRepositoryEmpty() || !isAllSitesIndexed())
                throw new AppHelperException("Сайты не проиндексированы");
        } else {
            if (!site.get().getStatus().equals(StatusType.INDEXED))
                throw new AppHelperException("Заданый сайт не проиндесирован");
        }
    }

    @NotNull
    private List<SiteEntity> createListSites(@NotNull Optional<SiteEntity> siteOptional) {
        return siteOptional.isEmpty() ?
                siteRepository.findAll() :
                new ArrayList<>(List.of(siteOptional.get()));
    }

    @NotNull
    private List<SearchRequest> createListSearchRequest(
            String query,
            int offset,
            int limit,
            @NotNull List<SiteEntity> sites) {
        List<SearchRequest> searchRequests = new ArrayList<>();
        Set<String> lemmasQuery = lemmaFinder.getLemmaSet(query);
        for (SiteEntity site : sites) {
            searchRequests.add(SearchRequest.builder()
                    .query(lemmasQuery)
                    .offset(offset)
                    .limit(limit)
                    .site(site)
                    .build());
        }
        return searchRequests;
    }

    private boolean isSiteRepositoryEmpty() {
        return siteRepository.count() == 0;
    }

    private boolean isAllSitesIndexed() {
        return siteRepository.getCountNoIndexedSites() == 0;
    }
}
