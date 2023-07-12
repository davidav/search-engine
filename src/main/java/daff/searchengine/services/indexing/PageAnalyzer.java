package daff.searchengine.services.indexing;

import daff.searchengine.exceptions.AppHelperException;
import daff.searchengine.models.IndexEntity;
import daff.searchengine.models.LemmaEntity;
import daff.searchengine.models.PageEntity;
import daff.searchengine.models.SiteEntity;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.util.LemmaFinder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;


@Slf4j
public class PageAnalyzer extends RecursiveTask<Boolean> {

    private static final int CODE_OK = 200;
    private static final int DELAY = 200;
    private final String[] wrongUrlSymbols = new String[]{"#", "term"};
    private final String[] fileExtensions = new String[]{".pdf", ".webp", ".jpg", ".jpeg", ".png", ".bmp", ".gif",
            ".mp3", ".mp4", ".avi", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".rar"};
    private final URL url;
    private final SiteEntity site;
    private final CopyOnWriteArrayList<String> siteLinks;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;
    private List<LemmaEntity> lemmas = new ArrayList<>();
    private List<IndexEntity> indexes = new ArrayList<>();
    private List<PageAnalyzer> tasks = new ArrayList<>();

    public PageAnalyzer(URL url,
                        SiteEntity site,
                        LemmaFinder lemmaFinder,
                        CopyOnWriteArrayList<String> siteLinks,
                        PageRepository pageRepository,
                        LemmaRepository lemmaRepository,
                        IndexRepository indexRepository) {
        this.url = url;
        this.site = site;
        this.lemmaFinder = lemmaFinder;
        this.siteLinks = siteLinks;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    protected Boolean compute() {
        log.info("start parsing {}", url);
        if (isThreadInterrupted())
            return false;
        try {
            Thread.sleep(DELAY);
            Document doc = Jsoup.connect(url.toString())
                    .userAgent(site.getAgent())
                    .referrer(site.getReferer())
                    .followRedirects(false)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .get();
            PageEntity page = PageEntity.builder()
                    .code(doc.connection().response().statusCode())
                    .path(url.getPath())
                    .content(doc.html())
                    .site(site)
                    .build();
            siteLinks.add(url.toString());
            if (page.getCode() == CODE_OK) {
                createLemmasAndIndexes(page);
            } else {
                log.info("response code - {} , page {} not analysis", page.getCode(), url);
            }
            saveData(page);
            tasks = createTasks(doc);
        } catch (InterruptedException e) {
            log.warn("Interrupted! PageAnalyzer ", e);
            Thread.currentThread().interrupt();
        } catch (HttpStatusException e) {
            log.warn("HttpStatus PageAnalyzer ", e);
            PageEntity pageError = PageEntity.builder()
                    .site(site)
                    .code(e.getStatusCode())
                    .path(url.getPath())
                    .build();
            pageRepository.save(pageError);
        } catch (IOException e) {
            log.warn("IOException PageAnalyzer ", e);
        }
        tasks.forEach(ForkJoinTask::join);
        log.info("end parsing {}", url);
        return !isThreadInterrupted();
    }

    private void createLemmasAndIndexes(@NotNull PageEntity page) {
        Map<String, Float> lemmaAndAmount =
                lemmaFinder.collectLemmas(Jsoup.parse(page.getContent()).text());
        lemmas = createLemmas(lemmaAndAmount);
        indexes = createIndexes(lemmaAndAmount);
    }

    @NotNull
    private List<LemmaEntity> createLemmas(@NotNull Map<String, Float> lemmasAndAmount) {
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        Set<String> lemmasSet = lemmasAndAmount.keySet();
        lemmasSet.forEach(l -> lemmaEntities.add(
                LemmaEntity.builder()
                        .frequency(1)
                        .lemma(l)
                        .site(site)
                        .build()));
        return lemmaEntities;
    }

    @NotNull
    private List<IndexEntity> createIndexes(@NotNull Map<String, Float> lemmasAndAmount) {
        final List<IndexEntity> indexEntities = new ArrayList<>();
        Set<Map.Entry<String, Float>> entrySet = lemmasAndAmount.entrySet();
        entrySet.forEach(l ->
                lemmas.forEach(lemma -> {
                    if (lemma.getLemma().equals(l.getKey())) {
                        indexEntities.add(IndexEntity.builder()
                                .rank(l.getValue())
                                .build());
                    }
                }));
        return indexEntities;
    }

    private void saveData(PageEntity page) {
        page = pageRepository.save(page);
        if (!lemmas.isEmpty()) {
            synchronized (lemmaRepository) {
                lemmas = lemmaRepository.saveAllAndFlush(lemmas);
                indexes = saveIndexes(page);
            }
        }
    }

    @NotNull
    private List<IndexEntity> saveIndexes(PageEntity page) {
        List<IndexEntity> indexEntities = new ArrayList<>();
        for (int i = 0; i < indexes.size(); i++) {
            IndexEntity index = indexes.get(i);
            index.setPageId(page.getId());
            index.setLemmaId(lemmas.get(i).getId());
            indexEntities.add(index);
        }
        return indexRepository.saveAllAndFlush(indexEntities);
    }

    @NotNull
    private List<PageAnalyzer> createTasks(@NotNull Document doc) {
        List<PageAnalyzer> tasksFromPage = new ArrayList<>();
        Elements elements = doc.select("body").select("a");
        for (Element a : elements) {
            String href = a.absUrl("href").replaceAll("\\?.+", "");
            if (isValidateLink(href) && !siteLinks.contains(href)) {
                siteLinks.add(href);
                try {
                    URL urlHref = new URL(href);

                    PageAnalyzer task = new PageAnalyzer(
                            urlHref,
                            site,
                            lemmaFinder,
                            siteLinks,
                            pageRepository,
                            lemmaRepository,
                            indexRepository
                    );
                    task.fork();
                    tasksFromPage.add(task);
                } catch (MalformedURLException e) {
                    throw new AppHelperException("bad urlPage" + e);
                }
            }

        }
        return tasksFromPage;
    }

    private boolean isValidateLink(String href) {
        href = href.toLowerCase(Locale.ROOT);
        if (!href.startsWith(site.getUrl())) {
            return false;
        }
        for (String wrongUrlSymbol : wrongUrlSymbols) {
            if (href.contains(wrongUrlSymbol)) {
                return false;
            }
        }
        for (String fileExtension : fileExtensions) {
            if (href.endsWith(fileExtension)) {
                return false;
            }
        }
        return true;
    }

    private boolean isThreadInterrupted() {
        if (Thread.currentThread().isInterrupted() || IndexingServiceImpl.isStop()) {
            log.info("interrupt {} {}", url, site.getName());
            Thread.currentThread().interrupt();
            return true;
        } else {
            return false;
        }
    }
}
