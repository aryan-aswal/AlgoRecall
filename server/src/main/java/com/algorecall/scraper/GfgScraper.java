package com.algorecall.scraper;

import com.algorecall.model.Problem;
import com.algorecall.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GfgScraper {

    private static final String GFG_API_URL = "https://practiceapi.geeksforgeeks.org/api/vr/problems/";
    private static final String GFG_PROBLEM_BASE = "https://www.geeksforgeeks.org/problems/";
    private static final String PLATFORM = "GFG";

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int scrapeProblems() {
        log.info("Starting GeeksForGeeks problem import via API...");
        int totalImported = 0;
        int totalPages = Integer.MAX_VALUE;

        RestTemplate restTemplate = new RestTemplate();

        for (int page = 0; page < totalPages; page++) {
            try {
                String url = GFG_API_URL + "?pageMode=explore&page=" + page + "&sortBy=submissions";

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                headers.set("Accept", "application/json");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                String json = response.getBody();
                if (json == null) break;

                JsonNode root = objectMapper.readTree(json);

                // Use "total" field (not "count" which is per-page size)
                if (page == 0 && root.has("total")) {
                    int totalProblems = root.get("total").asInt(0);
                    int perPage = root.has("count") ? root.get("count").asInt(20) : 20;
                    if (perPage > 0) {
                        totalPages = (totalProblems + perPage - 1) / perPage;
                    }
                    log.info("GFG API reports {} total problems, {} per page, ~{} pages", totalProblems, perPage, totalPages);
                }

                JsonNode results = root.has("results") ? root.get("results") : null;
                if (results == null || !results.isArray() || results.isEmpty()) {
                    log.info("No more GFG results at page {}", page);
                    break;
                }

                List<Problem> batch = new ArrayList<>();

                for (JsonNode item : results) {
                    try {
                        String title = item.has("problem_name") ? item.get("problem_name").asText() : null;
                        String slug = item.has("slug") ? item.get("slug").asText() : null;

                        if (title == null || title.isBlank()) continue;

                        // problem_url is the full URL from the API
                        String problemUrl = item.has("problem_url") ? item.get("problem_url").asText() : null;
                        if (problemUrl == null || problemUrl.isBlank()) {
                            // fallback: build from slug
                            if (slug == null || slug.isBlank()) continue;
                            problemUrl = GFG_PROBLEM_BASE + slug + "/1";
                        }

                        if (problemRepository.existsByUrl(problemUrl)) continue;

                        String diffText = item.has("difficulty") ? item.get("difficulty").asText("").toUpperCase() : "";
                        Problem.Difficulty difficulty = switch (diffText) {
                            case "EASY", "BASIC", "SCHOOL" -> Problem.Difficulty.EASY;
                            case "MEDIUM" -> Problem.Difficulty.MEDIUM;
                            case "HARD" -> Problem.Difficulty.HARD;
                            default -> null;
                        };

                        // tags is an object: { topic_tags: [...], company_tags: [...] }
                        String tags = null;
                        if (item.has("tags") && item.get("tags").isObject()) {
                            JsonNode tagsObj = item.get("tags");
                            if (tagsObj.has("topic_tags") && tagsObj.get("topic_tags").isArray()) {
                                List<String> tagList = new ArrayList<>();
                                for (JsonNode tag : tagsObj.get("topic_tags")) {
                                    String t = tag.asText("").trim();
                                    if (!t.isEmpty()) tagList.add(t);
                                }
                                if (!tagList.isEmpty()) tags = String.join(",", tagList);
                            }
                        }

                        Problem problem = Problem.builder()
                                .title(title)
                                .slug(slug)
                                .url(problemUrl)
                                .platform(PLATFORM)
                                .difficulty(difficulty)
                                .topicTags(tags)
                                .build();

                        batch.add(problem);
                    } catch (Exception e) {
                        log.debug("Skipping GFG problem: {}", e.getMessage());
                    }
                }

                if (!batch.isEmpty()) {
                    problemRepository.saveAll(batch);
                    totalImported += batch.size();
                    log.info("GFG page {}: imported {} problems", page, batch.size());
                }

            } catch (Exception e) {
                log.warn("GFG page {} failed: {}", page, e.getMessage());
                if (page > 3) continue; // skip failed pages after initial pages
                break;
            }
        }

        log.info("GFG import completed: {} total problems imported", totalImported);
        return totalImported;
    }
}
