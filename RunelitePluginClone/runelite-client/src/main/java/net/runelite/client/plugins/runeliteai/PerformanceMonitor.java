/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static net.runelite.client.plugins.runeliteai.AnalysisResults.PerformanceMetrics;

/**
 * Performance Monitor for real-time system monitoring
 * 
 * Tracks and analyzes:
 * - Processing time per tick and component
 * - Memory usage and garbage collection
 * - CPU utilization and thread activity
 * - Database response times
 * - Cache hit rates and efficiency
 * - Performance warnings and alerts
 * 
 * Provides detailed performance analytics for optimization
 * and ensures the plugin meets performance targets.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class PerformanceMonitor
{
    // JVM monitoring
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final Runtime runtime;
    
    // Performance tracking
    private final Map<String, AtomicLong> componentTimings = new ConcurrentHashMap<>();
    private final Queue<Long> recentProcessingTimes = new LinkedList<>();
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong totalTicks = new AtomicLong(0);
    
    // Memory tracking
    private final AtomicLong maxMemoryObserved = new AtomicLong(0);
    private final Queue<Long> memoryUsageHistory = new LinkedList<>();
    
    // GC tracking
    private long lastGCCount = 0;
    private long lastGCTime = 0;
    
    // Database performance
    private final AtomicLong totalDatabaseCalls = new AtomicLong(0);
    private final AtomicLong totalDatabaseTime = new AtomicLong(0);
    private final Queue<Long> recentDatabaseTimes = new LinkedList<>();
    
    // Cache performance
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // Performance warnings
    private final List<String> performanceWarnings = new ArrayList<>();
    private long lastWarningCleanup = System.currentTimeMillis();
    
    // Configuration
    private static final int MAX_HISTORY_SIZE = 1000;
    private static final long WARNING_CLEANUP_INTERVAL = 300000; // 5 minutes
    private static final double TARGET_PROCESSING_TIME_MS = 2.0;
    private static final double WARNING_PROCESSING_TIME_MS = 5.0;
    private static final double CRITICAL_PROCESSING_TIME_MS = 10.0;
    
    public PerformanceMonitor()
    {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.runtime = Runtime.getRuntime();
        
        // Initialize GC baseline
        updateGCBaseline();
        
        log.info("PerformanceMonitor initialized");
    }
    
    /**
     * Record tick processing time
     * @param processingTimeNanos Processing time in nanoseconds
     */
    public void recordTickProcessingTime(long processingTimeNanos)
    {
        totalProcessingTime.addAndGet(processingTimeNanos);
        totalTicks.incrementAndGet();
        
        // Track recent processing times
        recentProcessingTimes.offer(processingTimeNanos);
        while (recentProcessingTimes.size() > MAX_HISTORY_SIZE) {
            recentProcessingTimes.poll();
        }
        
        // Check for performance warnings
        double processingTimeMs = processingTimeNanos / 1_000_000.0;
        checkProcessingTimeWarnings(processingTimeMs);
        
        // Update memory usage
        updateMemoryTracking();
    }
    
    /**
     * Record component timing
     * @param componentName Name of the component
     * @param processingTimeNanos Processing time in nanoseconds
     */
    public void recordComponentTime(String componentName, long processingTimeNanos)
    {
        componentTimings.computeIfAbsent(componentName, k -> new AtomicLong(0))
            .addAndGet(processingTimeNanos);
    }
    
    /**
     * Record database operation timing
     * @param operationTimeNanos Database operation time in nanoseconds
     */
    public void recordDatabaseTime(long operationTimeNanos)
    {
        totalDatabaseCalls.incrementAndGet();
        totalDatabaseTime.addAndGet(operationTimeNanos);
        
        recentDatabaseTimes.offer(operationTimeNanos);
        while (recentDatabaseTimes.size() > MAX_HISTORY_SIZE) {
            recentDatabaseTimes.poll();
        }
    }
    
    /**
     * Record cache hit
     */
    public void recordCacheHit()
    {
        cacheHits.incrementAndGet();
    }
    
    /**
     * Record cache miss
     */
    public void recordCacheMiss()
    {
        cacheMisses.incrementAndGet();
    }
    
    /**
     * Get current performance metrics
     * @return Current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics()
    {
        try {
            // Calculate processing metrics
            long avgProcessingTimeNanos = totalTicks.get() > 0 ? 
                totalProcessingTime.get() / totalTicks.get() : 0;
            
            // Get memory metrics
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            // Calculate CPU usage (approximation)
            double cpuUsage = estimateCPUUsage();
            
            // Get thread count
            int activeThreads = Thread.activeCount();
            
            // Calculate database response time
            long avgDatabaseTime = totalDatabaseCalls.get() > 0 ? 
                totalDatabaseTime.get() / totalDatabaseCalls.get() : 0;
            
            // Calculate cache hit rate
            long totalCacheAccess = cacheHits.get() + cacheMisses.get();
            int cacheHitRate = totalCacheAccess > 0 ? 
                (int) ((cacheHits.get() * 100) / totalCacheAccess) : 0;
            
            // Get GC metrics
            GCMetrics gcMetrics = getCurrentGCMetrics();
            
            // Build component timings map
            Map<String, Long> componentTimingsMap = new HashMap<>();
            componentTimings.forEach((key, value) -> 
                componentTimingsMap.put(key, value.get() / 1_000_000)); // Convert to ms
            
            // Check if within performance targets
            boolean withinTargets = (avgProcessingTimeNanos / 1_000_000.0) <= TARGET_PROCESSING_TIME_MS;
            
            // Clean old warnings
            cleanupOldWarnings();
            
            return PerformanceMetrics.builder()
                .processingTimeNanos(avgProcessingTimeNanos)
                .memoryUsed(usedMemory)
                .maxMemoryAvailable(maxMemory)
                .cpuUsagePercent(cpuUsage)
                .activeThreads(activeThreads)
                .databaseResponseTime(avgDatabaseTime / 1_000_000) // Convert to ms
                .cacheHitRate(cacheHitRate)
                .gcCount(gcMetrics.gcCount)
                .gcTime(gcMetrics.gcTime)
                .componentTimings(componentTimingsMap)
                .performanceWarnings(new ArrayList<>(performanceWarnings))
                .withinPerformanceTargets(withinTargets)
                .build();
                
        } catch (Exception e) {
            log.error("Error collecting performance metrics", e);
            return createErrorMetrics();
        }
    }
    
    /**
     * Get performance summary for logging
     * @return Performance summary string
     */
    public String getPerformanceSummary()
    {
        long avgProcessingTimeMs = totalTicks.get() > 0 ? 
            (totalProcessingTime.get() / totalTicks.get()) / 1_000_000 : 0;
        
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        
        long totalCacheAccess = cacheHits.get() + cacheMisses.get();
        int cacheHitRate = totalCacheAccess > 0 ? 
            (int) ((cacheHits.get() * 100) / totalCacheAccess) : 0;
        
        return String.format(
            "Performance Summary: Avg Processing=%dms, Memory=%dMB/%dMB, " +
            "Cache Hit Rate=%d%%, Warnings=%d",
            avgProcessingTimeMs, usedMemoryMB, maxMemoryMB, 
            cacheHitRate, performanceWarnings.size()
        );
    }
    
    /**
     * Check if performance is within acceptable limits
     * @return True if performance is acceptable
     */
    public boolean isPerformanceAcceptable()
    {
        if (totalTicks.get() < 10) return true; // Not enough data
        
        long avgProcessingTimeMs = (totalProcessingTime.get() / totalTicks.get()) / 1_000_000;
        long usedMemoryPercent = ((runtime.totalMemory() - runtime.freeMemory()) * 100) / runtime.maxMemory();
        
        return avgProcessingTimeMs <= WARNING_PROCESSING_TIME_MS && usedMemoryPercent < 80;
    }
    
    /**
     * Reset performance tracking
     */
    public void resetTracking()
    {
        totalProcessingTime.set(0);
        totalTicks.set(0);
        totalDatabaseCalls.set(0);
        totalDatabaseTime.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        
        recentProcessingTimes.clear();
        memoryUsageHistory.clear();
        recentDatabaseTimes.clear();
        componentTimings.clear();
        performanceWarnings.clear();
        
        updateGCBaseline();
        
        log.info("Performance tracking reset");
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Check for processing time warnings
     */
    private void checkProcessingTimeWarnings(double processingTimeMs)
    {
        if (processingTimeMs > CRITICAL_PROCESSING_TIME_MS) {
            addPerformanceWarning("CRITICAL: Processing time " + 
                String.format("%.2fms", processingTimeMs) + " exceeds critical threshold");
        } else if (processingTimeMs > WARNING_PROCESSING_TIME_MS) {
            addPerformanceWarning("WARNING: Processing time " + 
                String.format("%.2fms", processingTimeMs) + " exceeds warning threshold");
        }
    }
    
    /**
     * Update memory tracking
     */
    private void updateMemoryTracking()
    {
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        maxMemoryObserved.updateAndGet(current -> Math.max(current, currentMemory));
        
        memoryUsageHistory.offer(currentMemory);
        while (memoryUsageHistory.size() > MAX_HISTORY_SIZE) {
            memoryUsageHistory.poll();
        }
        
        // Check for memory warnings
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (currentMemory * 100.0) / maxMemory;
        
        if (memoryUsagePercent > 90) {
            addPerformanceWarning("CRITICAL: Memory usage " + 
                String.format("%.1f%%", memoryUsagePercent) + " approaching limit");
        } else if (memoryUsagePercent > 80) {
            addPerformanceWarning("WARNING: Memory usage " + 
                String.format("%.1f%%", memoryUsagePercent) + " is high");
        }
    }
    
    /**
     * Estimate CPU usage (simplified approach)
     */
    private double estimateCPUUsage()
    {
        if (totalTicks.get() < 10) return 0.0;
        
        // Rough estimation based on processing time vs available time
        long avgProcessingTimeNanos = totalProcessingTime.get() / totalTicks.get();
        long availableTimeNanos = 600_000_000; // ~600ms per tick
        
        double cpuUsage = (avgProcessingTimeNanos * 100.0) / availableTimeNanos;
        return Math.min(100.0, Math.max(0.0, cpuUsage));
    }
    
    /**
     * Get current GC metrics
     */
    private GCMetrics getCurrentGCMetrics()
    {
        long totalGCCount = 0;
        long totalGCTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean.getCollectionCount() > 0) {
                totalGCCount += gcBean.getCollectionCount();
                totalGCTime += gcBean.getCollectionTime();
            }
        }
        
        // Calculate delta since last check
        long deltaGCCount = totalGCCount - lastGCCount;
        long deltaGCTime = totalGCTime - lastGCTime;
        
        // Update baselines
        lastGCCount = totalGCCount;
        lastGCTime = totalGCTime;
        
        return new GCMetrics((int) deltaGCCount, deltaGCTime);
    }
    
    /**
     * Update GC baseline
     */
    private void updateGCBaseline()
    {
        lastGCCount = 0;
        lastGCTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean.getCollectionCount() > 0) {
                lastGCCount += gcBean.getCollectionCount();
                lastGCTime += gcBean.getCollectionTime();
            }
        }
    }
    
    /**
     * Add performance warning
     */
    private void addPerformanceWarning(String warning)
    {
        synchronized (performanceWarnings) {
            performanceWarnings.add(System.currentTimeMillis() + ": " + warning);
            
            // Limit warning history
            while (performanceWarnings.size() > 50) {
                performanceWarnings.remove(0);
            }
        }
    }
    
    /**
     * Clean up old warnings
     */
    private void cleanupOldWarnings()
    {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastWarningCleanup > WARNING_CLEANUP_INTERVAL) {
            synchronized (performanceWarnings) {
                // Remove warnings older than 15 minutes
                long cutoff = currentTime - 900000;
                performanceWarnings.removeIf(warning -> {
                    try {
                        String[] parts = warning.split(":", 2);
                        long warningTime = Long.parseLong(parts[0]);
                        return warningTime < cutoff;
                    } catch (Exception e) {
                        return true; // Remove malformed warnings
                    }
                });
            }
            lastWarningCleanup = currentTime;
        }
    }
    
    /**
     * Create error metrics when collection fails
     */
    private PerformanceMetrics createErrorMetrics()
    {
        return PerformanceMetrics.builder()
            .processingTimeNanos(0L)
            .memoryUsed(0L)
            .maxMemoryAvailable(runtime.maxMemory())
            .cpuUsagePercent(0.0)
            .activeThreads(0)
            .databaseResponseTime(0L)
            .cacheHitRate(0)
            .gcCount(0)
            .gcTime(0L)
            .componentTimings(new HashMap<>())
            .performanceWarnings(List.of("Performance metrics collection failed"))
            .withinPerformanceTargets(false)
            .build();
    }
    
    // Click intelligence performance tracking removed - using existing metrics
    
    /**
     * Internal class for GC metrics
     */
    private static class GCMetrics
    {
        final int gcCount;
        final long gcTime;
        
        GCMetrics(int gcCount, long gcTime)
        {
            this.gcCount = gcCount;
            this.gcTime = gcTime;
        }
    }
}