package daff.searchengine.controllers;


import daff.searchengine.dto.ResultDTO;
import daff.searchengine.dto.search.SearchResult;
import daff.searchengine.dto.statistics.Detailed;
import daff.searchengine.dto.statistics.Statistics;
import daff.searchengine.dto.statistics.StatisticsDTO;
import daff.searchengine.dto.statistics.Total;
import daff.searchengine.services.indexing.IndexingService;
import daff.searchengine.services.search.SearchService;
import daff.searchengine.services.statistics.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest
class ApiControllerTest {

    @Autowired
    ApiController controller;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    StatisticsService statisticsService;
    @MockBean
    IndexingService indexingService;
    @MockBean
    SearchService searchService;

    @Test
    @DisplayName("statistics ok")
    void getStatistics() throws Exception {
        StatisticsDTO statisticsDTO = new StatisticsDTO(true, Statistics.builder()
                .total(Total.builder().sites(2L).build())
                .detailed(new ArrayList<>(List.of(
                        Detailed.builder().build(),
                        Detailed.builder().build())))
                .build());

        when(statisticsService.getStatistics()).thenReturn(statisticsDTO);

        this.mockMvc.perform(get("/api/statistics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics.total.sites",
                        is(Math.toIntExact(statisticsDTO.getStatistics().getTotal().getSites()))))
                .andExpect(jsonPath("$.statistics.detailed.size()",
                        is(statisticsDTO.getStatistics().getDetailed().size())));
        verify(statisticsService, Mockito.times(1))
                .getStatistics();
    }

    @Test
    @DisplayName("start indexing")
    void startIndexing() throws Exception {
        this.mockMvc.perform(get("/api/startIndexing"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(indexingService, Mockito.times(1))
                .startIndexing();
    }

    @Test
    void stopIndexing() throws Exception {
        this.mockMvc.perform(get("/api/stopIndexing"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(indexingService, Mockito.times(1))
                .stopIndexing();
    }

    @Test
    void indexPage() throws Exception {
        ResultDTO resultDTO = new ResultDTO(true);

        when(indexingService.indexPage(any(String.class))).thenReturn(resultDTO);

        this.mockMvc.perform(post("/api/indexPage")
                        .param("url", "http://url"))
                .andDo(print())
                .andExpect(status().isOk());
        verify(indexingService, Mockito.times(1))
                .indexPage(any(String.class));
    }

    @Test
    void search() throws Exception {
        ResultDTO resultDTO = new ResultDTO(true, 2,new ArrayList<>(List.of(
                SearchResult.builder().build(),
                SearchResult.builder().build())
                ));

        when(searchService.search(
                any(String.class),
                any(Integer.class),
                any(Integer.class),
                any(String.class)))
                .thenReturn(resultDTO);

        this.mockMvc.perform(get("/api/search")
                        .param("query", "страница")
                        .param("offset", "0")
                        .param("limit", "10")
                        .param("site","url"))
                .andDo(print())
                .andExpect(status().isOk());
        verify(searchService, Mockito.times(1))
                .search(any(String.class),
                        any(Integer.class),
                        any(Integer.class),
                        any(String.class));
    }

}