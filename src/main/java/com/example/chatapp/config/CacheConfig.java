package com.example.chatapp.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ROOMS_CACHE = "rooms";
    public static final String ROOM_DETAILS_CACHE = "roomDetails";
    public static final String ROOM_MEMBERS_CACHE = "roomMembers";
    public static final String USER_ROOMS_CACHE = "userRooms";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                ROOMS_CACHE,
                ROOM_DETAILS_CACHE,
                ROOM_MEMBERS_CACHE,
                USER_ROOMS_CACHE
        ));
        return cacheManager;
    }
}
