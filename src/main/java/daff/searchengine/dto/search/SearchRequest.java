package daff.searchengine.dto.search;


import daff.searchengine.models.SiteEntity;
import lombok.*;

import java.util.Set;

@Builder
@Getter
@Setter
public class SearchRequest {
    private final Set<String> query;
    private final int offset;
    private final int limit;
    private final SiteEntity site;

}
