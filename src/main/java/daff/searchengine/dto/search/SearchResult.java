package daff.searchengine.dto.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Builder
@Getter
@Setter
public class SearchResult implements Comparable<SearchResult>{
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    public SearchResult(String site, String siteName, String uri, String title, String snippet, float relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    @Override
    public int compareTo(@NotNull SearchResult o) {
        if ((relevance - o.relevance) == 0) return 0;
        return ((relevance - o.relevance) > 0) ? -1 : 1;
    }
}

