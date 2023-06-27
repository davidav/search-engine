package daff.searchengine.dto.statistics;


import daff.searchengine.models.StatusType;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class Detailed {

    private String url;
    private String name;
    private StatusType status;
    private LocalDateTime statusTime;
    private String error;
    private Integer pages = 0;
    private Integer lemmas = 0;

}
