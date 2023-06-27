package daff.searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class StatisticsDTO {
    private boolean result;
    private String error;
    private Statistics statistics;


    public StatisticsDTO(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public StatisticsDTO(boolean result, Statistics statistics) {
        this.result = result;
        this.statistics = statistics;
    }
}