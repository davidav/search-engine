package daff.searchengine.dto.statistics;


import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
public class Statistics {

    private Total total;
    private List<Detailed> detailed;

}
