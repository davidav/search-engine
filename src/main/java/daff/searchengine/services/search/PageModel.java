package daff.searchengine.services.search;

import daff.searchengine.models.SiteEntity;
import lombok.*;
import org.jetbrains.annotations.NotNull;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageModel implements Comparable<PageModel> {
    private int id;
    private String path;
    private int code;
    private String content;
    private SiteEntity site;
    private float absRelevance;
    private float relevance;

    @Override
    public String toString() {
        return "PageModel = " + id;
    }

    @Override
    public int compareTo(@NotNull PageModel o) {
        if ((relevance - o.relevance) == 0) return 0;
        return ((relevance - o.relevance) > 0) ? -1 : 1;
    }
}
