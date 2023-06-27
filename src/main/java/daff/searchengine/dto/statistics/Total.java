package daff.searchengine.dto.statistics;

import lombok.*;
@Builder
@Getter
@Setter
public class Total {
    private Long sites;
    private Long pages;
    private int lemmas;
    private boolean isIndexing = false;
}

