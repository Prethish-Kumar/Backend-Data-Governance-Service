package com.complyance.Data_Governance_Service.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final long startTime = System.currentTimeMillis();
    private final MongoTemplate mongoTemplate;

    public SystemController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        Duration uptime = Duration.ofMillis(rb.getUptime());

        metrics.put("uptimeMillis", uptime.toMillis());
        metrics.put("uptimeHuman", formatDuration(uptime));
        metrics.put("memoryUsedMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        metrics.put("memoryFreeMB", runtime.freeMemory() / (1024 * 1024));
        metrics.put("cpuCount", runtime.availableProcessors());
        metrics.put("startTime", startTime);

        // 🧩 MongoDB stats
        Map<String, Object> dbStats = new HashMap<>();
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            long count = mongoTemplate.getCollection(collectionName).countDocuments();
            dbStats.put(collectionName, count);
        }

        metrics.put("totalCollections", dbStats.size());
        metrics.put("collections", dbStats);

        return ResponseEntity.ok(metrics);
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.minusHours(hours).toMinutes();
        long seconds = d.minusHours(hours).minusMinutes(minutes).getSeconds();
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
}
