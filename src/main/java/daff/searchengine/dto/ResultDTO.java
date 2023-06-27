package daff.searchengine.dto;

import daff.searchengine.dto.search.SearchResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResultDTO {

    private boolean result;
    private String error;
    private int count = 0;
    private List<SearchResult> data;

    public ResultDTO() {
    }

    public ResultDTO(boolean result) {
        this.result = result;
    }

    public ResultDTO(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public ResultDTO(boolean result, int count, List<SearchResult>  data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }
}

