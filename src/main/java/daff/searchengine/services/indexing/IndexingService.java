package daff.searchengine.services.indexing;

import daff.searchengine.dto.ResultDTO;

public interface IndexingService {

    boolean isIndexing();
    ResultDTO startIndexing();
    ResultDTO stopIndexing();
    ResultDTO indexPage(String url);

}

