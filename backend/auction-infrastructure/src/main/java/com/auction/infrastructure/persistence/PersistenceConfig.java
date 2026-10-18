package com.auction.infrastructure.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA entities and Spring Data repositories live in this module, outside the application's base package.
 */
@Configuration
@EntityScan(basePackageClasses = PersistenceConfig.class)
@EnableJpaRepositories(basePackageClasses = PersistenceConfig.class)
public class PersistenceConfig {
}
