package com.algorecall.repository;

import com.algorecall.model.Problem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {

    List<Problem> findByDifficulty(Problem.Difficulty difficulty);

    List<Problem> findByPlatform(String platform);

    List<Problem> findByTopicTagsContainingIgnoreCase(String tag);

    List<Problem> findByTitleContainingIgnoreCase(String title);

    List<Problem> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR CAST(p.problemNumber AS string) LIKE CONCAT('%', :q, '%')")
    List<Problem> fuzzySearch(@Param("q") String query, Pageable pageable);

    Optional<Problem> findByPlatformAndProblemNumber(String platform, Integer problemNumber);

    boolean existsByPlatformAndProblemNumber(String platform, Integer problemNumber);

    boolean existsByUrl(String url);

    @Query(value = "SELECT DISTINCT trim(t) FROM problems, unnest(string_to_array(topic_tags, ',')) AS t WHERE topic_tags IS NOT NULL AND trim(t) <> '' ORDER BY trim(t)", nativeQuery = true)
    List<String> findDistinctTopics();

    @Query(value = "SELECT COUNT(DISTINCT p.id) FROM problems p, unnest(string_to_array(p.topic_tags, ',')) AS t WHERE LOWER(TRIM(t)) LIKE :pattern", nativeQuery = true)
    long countByTopicTagPattern(@Param("pattern") String pattern);
}
