package daff.searchengine.services.search;

import daff.searchengine.dto.search.SearchRequest;
import daff.searchengine.dto.search.SearchResult;
import daff.searchengine.models.IndexEntity;
import daff.searchengine.models.LemmaEntity;
import daff.searchengine.models.PageEntity;
import daff.searchengine.repo.IndexRepository;
import daff.searchengine.repo.LemmaRepository;
import daff.searchengine.repo.PageRepository;
import daff.searchengine.util.LemmaFinder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public record SearchSiteAnalyzer(SearchRequest searchRequest, CopyOnWriteArraySet<SearchResult> searchResults,
                                 LemmaFinder lemmaFinder, PageRepository pageRepository,
                                 LemmaRepository lemmaRepository, IndexRepository indexRepository)
        implements Callable<Boolean> {

    @Override
    @NotNull
    public  Boolean call() {
        List<LemmaEntity> lemmasRequest = lemmaRepository.findAllByLemmaInAndSiteId(
                searchRequest.getQuery(), searchRequest.getSite().getId());
        if (lemmasRequest.size() != searchRequest.getQuery().size() || lemmasRequest.isEmpty()) {
            return false;
        }
        List<Integer> lemmaRequestIds = new ArrayList<>();
        for (LemmaEntity lemmaEntity : lemmasRequest) {
            Integer id = lemmaEntity.getId();
            lemmaRequestIds.add(id);
        }
        List<IndexEntity> indexesRequest = indexRepository.findAllByLemmaIdIn(lemmaRequestIds);
        List<PageModel> pagesRequest = getPagesRequest(indexesRequest, lemmasRequest.size());
        if (pagesRequest.isEmpty()) {
            return false;
        }
        List<PageModel> pagesResponse = calcRelevance(pagesRequest, lemmasRequest, indexesRequest);
        Set<SearchResult> searchResultsSite = prepareResult(pagesResponse, searchRequest.getQuery());
        if (searchResultsSite.isEmpty()) {
            return false;
        }
        searchResults.addAll(searchResultsSite);
        return true;
    }

    @NotNull
    private synchronized List<PageModel> getPagesRequest(@NotNull List<IndexEntity> indexEntities, int countLemmas) {
        Map<Integer, Integer> countIndexForPage = new HashMap<>();
        for (IndexEntity indexEntity : indexEntities) {
            int pageId = indexEntity.getPageId();
            countIndexForPage.put(pageId,
                    countIndexForPage.get(pageId) == null ? 1 : countIndexForPage.get(pageId) + 1);
        }
        List<Integer> pageIds = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : countIndexForPage.entrySet()) {
            if (countLemmas == entry.getValue()) {
                pageIds.add(entry.getKey());
            }
        }
        List<PageEntity> byIdIn = pageRepository.findByIdIn(pageIds);
        return pagesEntityToPagesModel(byIdIn);
    }

    @NotNull
    private List<PageModel> calcRelevance(List<PageModel> pages, List<LemmaEntity> lemmas, List<IndexEntity> indexes) {
        float maxAbsRelevance = 0;
        for (PageModel page : pages) {
            float absRelevancePage = 0;
            for (LemmaEntity lemma : lemmas) {
                absRelevancePage += getRank(page, lemma, indexes);
            }
            page.setAbsRelevance(absRelevancePage);
            maxAbsRelevance = Math.max(maxAbsRelevance, absRelevancePage);
        }
        for (PageModel page : pages) {
            page.setRelevance(page.getAbsRelevance() / maxAbsRelevance);
        }
        return pages;
    }

    private float getRank(PageModel page, LemmaEntity lemma, @NotNull List<IndexEntity> indexes) {
        float rank = 0;
        for (IndexEntity indexEntity : indexes) {
            if (indexEntity.getPageId() == page.getId() && indexEntity.getLemmaId() == lemma.getId()) {
                rank = indexEntity.getRank();
            }
        }
        return rank;
    }

    @NotNull
    private List<PageModel> pagesEntityToPagesModel(@NotNull List<PageEntity> pageEntities) {
        List<PageModel> pageModels = new ArrayList<>();
        for (PageEntity pageEntity : pageEntities) {
            PageModel pageModel = PageModel.builder()
                    .id(pageEntity.getId())
                    .code(pageEntity.getCode())
                    .path(pageEntity.getPath())
                    .content(pageEntity.getContent())
                    .site(pageEntity.getSite())
                    .build();
            pageModels.add(pageModel);
        }
        return pageModels;
    }

    @NotNull
    private Set<SearchResult> prepareResult(@NotNull List<PageModel> pagesResponse, Set<String> lemmasQuery) {
        Set<SearchResult> results = new HashSet<>();
        for (PageModel pageModel : pagesResponse) {
            Document doc = Jsoup.parse(pageModel.getContent());
            results.add(SearchResult.builder()
                    .site(pageModel.getSite().getUrl())
                    .siteName(pageModel.getSite().getName())
                    .uri(pageModel.getPath())
                    .title(getTitle(doc))
                    .snippet(getSnippet(doc, lemmasQuery))
                    .relevance(pageModel.getRelevance())
                    .build());
        }
        return results;
    }

    private String getTitle(@NotNull Document doc) {
        String title = "";
        Elements titles = doc.select("title");
        for (Element titleHTML : titles) {
            title = titleHTML.html();
        }
        return title;
    }

    @NotNull
    private String getSnippet(@NotNull Document doc, @NotNull Set<String> lemmasQuery) {
        StringBuilder sb = new StringBuilder();
        String content = doc.select("body").text();
        String[] words = lemmaFinder.arrayContainsRussianWords(content);
        int count = 0;
        for (String lemma : lemmasQuery) {
            for (String word : words) {
                List<String> normalForms = lemmaFinder.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }
                String form = normalForms.stream().findFirst().get();
                if (form.equals(lemma)) {
                    if (count == 0) {
                        sb.append(createSnippet(content, word));
                    }
                    count++;
                }
            }
        }
        sb.append(count == 1 ? "" : "Найдено ещё " + (count - 1) + " совпадений на этой странице");
        return sb.toString();
    }

    @NotNull
    private String createSnippet(@NotNull String content, String word) {
        StringBuilder sb = new StringBuilder();
        int i = content.indexOf(word);
        String s = content.substring(Math.max((i - 100), 0), Math.min((i + 150), content.length()));
        sb.append(s.replace(word, "<b>" + word + "</b>")).append("... <br />");
        return sb.toString();
    }
}
