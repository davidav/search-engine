package daff.searchengine.controllers;


import daff.searchengine.dto.ResultDTO;
import daff.searchengine.dto.statistics.StatisticsDTO;
import daff.searchengine.services.indexing.IndexingService;
import daff.searchengine.services.search.SearchService;
import daff.searchengine.services.statistics.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService,
                         IndexingService indexingService,
                         SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsDTO> statistics() {
        log.info("Statistics out");
        try {
            return ResponseEntity.ok().body(statisticsService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.ok().body(new StatisticsDTO(false, e.getMessage()));
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResultDTO> startIndexing() {
        log.info("Indexing start");
        try {
            return ResponseEntity.ok().body(indexingService.startIndexing());
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultDTO(false, e.getMessage()));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResultDTO> stopIndexing() {
        log.info("Indexing stop");
        try {
            return ResponseEntity.ok().body(indexingService.stopIndexing());
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultDTO(false, e.getMessage()));
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResultDTO> indexPage(@RequestParam("url") String url) {
        log.info("Start analyze {}", url);
        try {
            return ResponseEntity.ok().body(indexingService.indexPage(url));
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultDTO(false, e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ResultDTO> search(@RequestParam(value = "query") String query,
                                            @RequestParam(value = "offset") int offset,
                                            @RequestParam(value = "limit") int limit,
                                            @RequestParam(value = "site", required = false) String site
    ) throws IOException {
        log.info("Start search - {}", query);
        try {
            return ResponseEntity.ok().body(searchService.search(query, offset, limit, site));
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultDTO(false, e.getMessage()));
        }
    }
}
