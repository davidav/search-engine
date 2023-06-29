package daff.searchengine.services.statistics;


import daff.searchengine.dto.statistics.Detailed;
import daff.searchengine.dto.statistics.Statistics;
import daff.searchengine.dto.statistics.StatisticsDTO;
import daff.searchengine.dto.statistics.Total;
import daff.searchengine.models.SiteEntity;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.repo.SiteRepository;
import daff.searchengine.services.indexing.IndexingService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Autowired
    public StatisticsServiceImpl(IndexingService indexingService,
                                 SiteRepository siteRepository,
                                 PageRepository pageRepository,
                                 IndexRepository indexRepository) {
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public StatisticsDTO getStatistics() {
        return new StatisticsDTO(true, Statistics.builder()
                .total(getTotal())
                .detailed(getDetailedList())
                .build());
    }

    private Total getTotal() {
            return Total.builder()
                    .sites(siteRepository.count())
                    .pages(pageRepository.count())
                    .lemmas(indexRepository.getSumRank() != null ?
                            indexRepository.getSumRank() : 0)
                    .isIndexing(indexingService.isIndexing())
                    .build();
    }

    private List<Detailed> getDetailedList() {
            List<SiteEntity> sites = siteRepository.findAll();
        List<Detailed> detaileds = new ArrayList<>();
        for (SiteEntity site : sites) {
            Detailed detailed = getDetailed(site);
            detaileds.add(detailed);
        }
        return detaileds;
    }

    private Detailed getDetailed(@NotNull SiteEntity site) {
        return Detailed.builder()
                .url(site.getUrl())
                .name(site.getName())
                .status(site.getStatus())
                .statusTime(site.getStatusTime())
                .error(site.getLastError())
                .pages(pageRepository.getCountBySite(site.getId()))
                .lemmas(indexRepository.getSumRankBySite(site.getId()))
                .build();
    }
}
