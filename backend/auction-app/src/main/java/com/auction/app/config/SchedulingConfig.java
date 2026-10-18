package com.auction.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} tasks (SSE heartbeats). */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
