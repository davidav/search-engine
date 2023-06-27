package daff.searchengine.services.search;

import daff.searchengine.dto.ResultDTO;

public interface SearchService {
    ResultDTO search(String query, int offset, int limit, String site);

}
