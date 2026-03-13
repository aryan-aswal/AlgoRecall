package com.algorecall.scraper;

import com.algorecall.model.Problem;
import com.algorecall.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeetCodeImporter {

    private static final String LEETCODE_API = "https://leetcode.com/api/problems/all/";
    private static final String LEETCODE_BASE_URL = "https://leetcode.com/problems/";
    private static final String PLATFORM = "LEETCODE";

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int importProblems() {
        log.info("Starting LeetCode problem import...");
        int importedCount = 0;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(LEETCODE_API, String.class);

            if (json == null) {
                log.warn("Received null response from LeetCode API");
                return 0;
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode statStatusPairs = root.get("stat_status_pairs");

            if (statStatusPairs == null || !statStatusPairs.isArray()) {
                log.warn("No stat_status_pairs found in LeetCode API response");
                return 0;
            }

            List<Problem> problemsToSave = new ArrayList<>();

            for (JsonNode pair : statStatusPairs) {
                JsonNode stat = pair.get("stat");
                JsonNode difficulty = pair.get("difficulty");

                if (stat == null) continue;

                Integer problemNumber = stat.has("frontend_question_id")
                        ? stat.get("frontend_question_id").asInt() : null;
                String title = stat.has("question__title")
                        ? stat.get("question__title").asText() : null;
                String slug = stat.has("question__title_slug")
                        ? stat.get("question__title_slug").asText() : null;

                if (problemNumber == null || title == null || slug == null) continue;

                // Skip duplicates
                if (problemRepository.existsByPlatformAndProblemNumber(PLATFORM, problemNumber)) {
                    continue;
                }

                int difficultyLevel = difficulty != null && difficulty.has("level")
                        ? difficulty.get("level").asInt() : 0;

                Problem.Difficulty diff = switch (difficultyLevel) {
                    case 1 -> Problem.Difficulty.EASY;
                    case 2 -> Problem.Difficulty.MEDIUM;
                    case 3 -> Problem.Difficulty.HARD;
                    default -> null;
                };

                Problem problem = Problem.builder()
                        .problemNumber(problemNumber)
                        .title(title)
                        .slug(slug)
                        .url(LEETCODE_BASE_URL + slug)
                        .platform(PLATFORM)
                        .difficulty(diff)
                        .build();

                problemsToSave.add(problem);
                importedCount++;
            }

            if (!problemsToSave.isEmpty()) {
                problemRepository.saveAll(problemsToSave);
                log.info("Imported {} LeetCode problems", importedCount);
            } else {
                log.info("No new LeetCode problems to import");
            }

        } catch (Exception e) {
            log.error("Error importing LeetCode problems: {}", e.getMessage(), e);
        }

        return importedCount;
    }

    /**
     * Fetch topic tags from LeetCode GraphQL API for a given problem slug.
     * Returns comma-separated tags, or null if unavailable.
     */
    public String fetchTopicTags(String slug) {
        if (slug == null || slug.isBlank()) return null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            String graphqlQuery = "{\"query\":\"query questionTopicTags($titleSlug: String!) { question(titleSlug: $titleSlug) { topicTags { name } } }\",\"variables\":{\"titleSlug\":\"" + slug.replace("\"", "") + "\"}}";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(graphqlQuery, headers);

            String response = restTemplate.postForObject("https://leetcode.com/graphql", entity, String.class);
            if (response == null) return null;

            JsonNode root = objectMapper.readTree(response);
            JsonNode tags = root.path("data").path("question").path("topicTags");
            if (tags.isMissingNode() || !tags.isArray() || tags.isEmpty()) return null;

            StringBuilder sb = new StringBuilder();
            for (JsonNode tag : tags) {
                if (sb.length() > 0) sb.append(",");
                sb.append(tag.get("name").asText());
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to fetch topic tags for slug '{}': {}", slug, e.getMessage());
            return null;
        }
    }

    /**
     * Ensure a problem has topic tags; if not, try to fetch them from LeetCode.
     */
    @Transactional
    public void ensureTopicTags(Problem problem) {
        if (problem == null || !PLATFORM.equals(problem.getPlatform())) return;
        if (problem.getTopicTags() != null && !problem.getTopicTags().isBlank()) return;

        String tags = fetchTopicTags(problem.getSlug());
        if (tags != null && !tags.isBlank()) {
            problem.setTopicTags(tags);
            problemRepository.save(problem);
            log.info("Updated topic tags for '{}': {}", problem.getTitle(), tags);
        }
    }
}
