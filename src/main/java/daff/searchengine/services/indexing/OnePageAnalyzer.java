package daff.searchengine.services.indexing;

import daff.searchengine.config.SiteConfig;
import daff.searchengine.config.SitesConfig;
import daff.searchengine.exceptions.AppHelperException;
import daff.searchengine.models.IndexEntity;
import daff.searchengine.models.LemmaEntity;
import daff.searchengine.models.PageEntity;
import daff.searchengine.models.SiteEntity;
import daff.searchengine.models.StatusType;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.repo.SiteRepository;
import daff.searchengine.util.LemmaFinder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Transactional(readOnly = true)
public class OnePageAnalyzer implements Runnable {
    private final URL url;
    private final SitesConfig sitesConfig;
    private final LemmaFinder lemmaFinder;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public OnePageAnalyzer(URL url,
                           SitesConfig sitesConfig,
                           LemmaFinder lemmaFinder,
                           SiteRepository siteRepository,
                           PageRepository pageRepository,
                           LemmaRepository lemmaRepository,
                           IndexRepository indexRepository) {
        this.url = url;
        this.sitesConfig = sitesConfig;
        this.lemmaFinder = lemmaFinder;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public void run() {
        SiteEntity site = prepareSite();
        Optional<PageEntity> pageOptional = pageRepository.findByPathAndSite(url.getPath(), site);
        pageOptional.ifPresent(this::correctData);
        PageEntity page = parsingPage(url, site);
        Map<String, Float> lemmasAndAmount =
                lemmaFinder.collectLemmas(Jsoup.parse(page.getContent()).text());
        List<LemmaEntity> lemmas = createLemmas(lemmasAndAmount, site);
        createIndexes(lemmasAndAmount, lemmas, page);
        log.info("end analyze {}", url);
    }

    private SiteEntity prepareSite() {
        SiteConfig siteConfig = sitesConfig.getSites().stream()
                .filter(s -> url.toString().startsWith(s.getUrl()))
                .findAny().orElseThrow(() ->
                        new AppHelperException("siteConfig not found"));
        SiteEntity site = SiteEntity.builder()
                .name(siteConfig.getName())
                .url(siteConfig.getUrl())
                .status(StatusType.INDEXING)
                .statusTime(LocalDateTime.now())
                .agent(sitesConfig.getAgent())
                .referer(sitesConfig.getReferer())
                .build();
        return siteRepository.findByUrl(site.getUrl()).isEmpty() ?
                siteRepository.save(site) :
                siteRepository.findByUrl(site.getUrl()).get();
    }

    private void correctData(@NotNull PageEntity page) {
        List<LemmaEntity> lemmasForUpdate = new ArrayList<>();
        List<LemmaEntity> lemmasForDelete = new ArrayList<>();

        List<IndexEntity> indexEntitiesForPage = indexRepository.findAllByPageId(page.getId());

        List<LemmaEntity> lemmaEntities = lemmaRepository.findAllByIdIn(
                indexEntitiesForPage.stream().map(IndexEntity::getLemmaId).toList());

        for (LemmaEntity l : lemmaEntities) {
            if (l.getFrequency() > 1) {
                l.setFrequency(l.getFrequency() - 1);
                lemmasForUpdate.add(l);
            } else {
                lemmasForDelete.add(l);
            }
        }
        lemmaRepository.saveAllAndFlush(lemmasForUpdate);
        indexRepository.deleteAllByPageId(page.getId());
        lemmaRepository.deleteAll(lemmasForDelete);
        pageRepository.delete(page);

    }

    @NotNull
    private PageEntity parsingPage(@NotNull URL url, SiteEntity site) {
        String path;
        int statusCode;
        String content;
        try {
            Connection.Response response = Jsoup.connect(url.toString())
                    .userAgent(sitesConfig.getAgent())
                    .referrer(sitesConfig.getReferer())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();
            statusCode = response.statusCode();
            Document doc = response.parse();
            content = doc.toString().replace('\"', '\'');
            path = url.getPath();
        } catch (IOException e) {
            throw new AppHelperException("IOException OnePageAnalyzer" + e.getMessage());
        }

        return pageRepository.save(PageEntity.builder()
                .path(path)
                .code(statusCode)
                .content(content)
                .site(site)
                .build());
    }

    @NotNull
    private List<LemmaEntity> createLemmas(@NotNull Map<String, Float> lemmasAndAmount, SiteEntity site) {
        List<LemmaEntity> lemmas = new ArrayList<>();
        Set<String> lemmasString = lemmasAndAmount.keySet();
        lemmasString.forEach(l -> lemmas.add(
                LemmaEntity.builder()
                        .frequency(1)
                        .lemma(l)
                        .site(site)
                        .build()));
        return lemmaRepository.saveAllAndFlush(lemmas);
    }

    private void createIndexes(@NotNull Map<String, Float> lemmasAndAmount, List<LemmaEntity> lemmas, PageEntity page) {
        final List<IndexEntity> indexes = new ArrayList<>();
        Set<Map.Entry<String, Float>> entrySet = lemmasAndAmount.entrySet();
        entrySet.forEach(l ->
                lemmas.forEach(lemma -> {
                    if (lemma.getLemma().equals(l.getKey())) {
                        indexes.add(IndexEntity.builder()
                                .rank(l.getValue())
                                .lemmaId(lemma.getId())
                                .pageId(page.getId())
                                .build());
                    }
                }));
        indexRepository.saveAllAndFlush(indexes);
    }
}
