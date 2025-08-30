/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service class for integrating with OSRS Wiki APIs for item names and game data
 * Uses the wiki's pricing API and MediaWiki API for comprehensive data lookups
 * 
 * @author RuneLiteAI Team
 * @version 1.0.0
 */
@Slf4j
public class OSRSWikiService {
    private static final String WIKI_PRICING_API = "https://prices.runescape.wiki/api/v1/osrs";
    private static final String WIKI_API_BASE = "https://oldschool.runescape.wiki/api.php";
    
    private static final int TIMEOUT_SECONDS = 3;
    private static final int CACHE_EXPIRY_MINUTES = 60;
    
    // HTTP client for API requests
    private final HttpClient httpClient;
    private final Gson gson;
    
    // In-memory cache for API responses to avoid repeated calls
    private final ConcurrentHashMap<String, CachedResponse> cache = new ConcurrentHashMap<>();
    
    /**
     * Cached response wrapper with timestamp for expiry
     */
    private static class CachedResponse {
        private final String data;
        private final long timestamp;
        
        public CachedResponse(String data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > TimeUnit.MINUTES.toMillis(CACHE_EXPIRY_MINUTES);
        }
        
        public String getData() {
            return data;
        }
    }
    
    public OSRSWikiService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Get item mapping data from Wiki pricing API
     * This provides item names, examine text, and other metadata
     * @return CompletableFuture containing JsonObject with all item mappings
     */
    public CompletableFuture<JsonObject> getItemMappings() {
        String cacheKey = "item_mappings";
        
        // Check cache first
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            try {
                JsonParser parser = new JsonParser();
                JsonObject mappings = parser.parse(cached.getData()).getAsJsonObject();
                return CompletableFuture.completedFuture(mappings);
            } catch (Exception e) {
                log.debug("Failed to parse cached item mappings: {}", e.getMessage());
            }
        }
        
        // Make API call
        return makeApiCall(WIKI_PRICING_API + "/mapping", cacheKey)
            .thenApply(response -> {
                if (response == null) return null;
                try {
                    JsonParser parser = new JsonParser();
                    return parser.parse(response).getAsJsonObject();
                } catch (Exception e) {
                    log.debug("Failed to parse item mappings: {}", e.getMessage());
                    return null;
                }
            })
            .exceptionally(throwable -> {
                log.debug("Wiki item mappings lookup failed: {}", throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Get item name from Wiki item mappings by item ID
     * @param itemId The item ID to lookup
     * @return CompletableFuture containing the item name, or null if not found
     */
    public CompletableFuture<String> getItemName(int itemId) {
        return getItemMappings().thenApply(mappings -> {
            if (mappings == null) return null;
            try {
                String itemIdStr = String.valueOf(itemId);
                if (mappings.has(itemIdStr)) {
                    JsonObject itemData = mappings.getAsJsonObject(itemIdStr);
                    if (itemData.has("name")) {
                        return itemData.get("name").getAsString();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to lookup item name for ID {}: {}", itemId, e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Get item examine text from Wiki item mappings
     * @param itemId The item ID to lookup
     * @return CompletableFuture containing the examine text, or null if not found
     */
    public CompletableFuture<String> getItemExamineText(int itemId) {
        return getItemMappings().thenApply(mappings -> {
            if (mappings == null) return null;
            try {
                String itemIdStr = String.valueOf(itemId);
                if (mappings.has(itemIdStr)) {
                    JsonObject itemData = mappings.getAsJsonObject(itemIdStr);
                    if (itemData.has("examine")) {
                        return itemData.get("examine").getAsString();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to lookup item examine for ID {}: {}", itemId, e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Make an HTTP API call to OSRS Wiki
     * @param url The full URL to call
     * @param cacheKey The cache key to store the response
     * @return CompletableFuture containing the response body
     */
    private CompletableFuture<String> makeApiCall(String url, String cacheKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("User-Agent", "RuneLiteAI/1.0 (Educational Research)")
                .GET()
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        // Cache the response
                        cache.put(cacheKey, new CachedResponse(body));
                        return body;
                    } else {
                        log.debug("Wiki API returned status {}: {}", response.statusCode(), url);
                        return null;
                    }
                })
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
        } catch (Exception e) {
            log.debug("Failed to create Wiki API request for URL {}: {}", url, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Clear expired cache entries to prevent memory bloat
     */
    public void cleanupCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("Wiki API cache cleanup completed. {} entries remaining.", cache.size());
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public String getCacheStats() {
        long expired = cache.values().stream().mapToLong(cached -> cached.isExpired() ? 1 : 0).sum();
        return String.format("Wiki Cache: %d total entries, %d expired", cache.size(), expired);
    }
    
    /**
     * Test the Wiki API connectivity
     * @return CompletableFuture containing true if API is accessible, false otherwise
     */
    public CompletableFuture<Boolean> testConnectivity() {
        return makeApiCall(WIKI_PRICING_API + "/mapping", "connectivity_test")
            .thenApply(response -> response != null)
            .exceptionally(throwable -> {
                log.debug("Wiki API connectivity test failed: {}", throwable.getMessage());
                return false;
            });
    }
}