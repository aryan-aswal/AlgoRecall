package com.algorecall.scraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataImportRunner implements CommandLineRunner {

    private final LeetCodeImporter leetCodeImporter;
    private final GfgScraper gfgScraper;

    @Value("${app.import.enabled:true}")
    private boolean importEnabled;

    @Override
    public void run(String... args) {
        if (!importEnabled) {
            log.info("Data import is disabled. Set app.import.enabled=true to enable.");
            return;
        }

        log.info("=== Starting data import at application startup ===");

        try {
            int leetcodeCount = leetCodeImporter.importProblems();
            log.info("LeetCode import completed: {} problems imported", leetcodeCount);
        } catch (Exception e) {
            log.error("LeetCode import failed: {}", e.getMessage(), e);
        }

        try {
            int gfgCount = gfgScraper.scrapeProblems();
            log.info("GFG scrape completed: {} problems imported", gfgCount);
        } catch (Exception e) {
            log.error("GFG scrape failed: {}", e.getMessage(), e);
        }

        log.info("=== Data import completed ===");
    }
}
