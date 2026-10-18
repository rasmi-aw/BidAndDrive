package com.auction.infrastructure.persistence;

import com.auction.infrastructure.InfrastructureIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The schema comes from the Flyway migrations (never from Hibernate), in tests as in production.
 */
class SchemaMigrationTest extends InfrastructureIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @Test
    void migrationsAreAppliedAndHibernateOnlyValidates() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(ddlAuto).isEqualTo("validate");
    }

    @Test
    void auctionsHaveAnOptimisticLockVersionAndEveryTableASoftDeleteFlag() {
        assertThat(columns("auctions")).contains("version", "deleted");
        assertThat(columns("users")).contains("deleted");
        assertThat(columns("bids")).contains("deleted");
    }

    @Test
    void queriedColumnsAreIndexed() {
        assertThat(indexDefinition("ix_auctions_status_end_date")).contains("(status, end_date)");
        assertThat(indexDefinition("ix_bids_auction_id")).contains("(auction_id)");
    }

    private List<String> columns(String table) {
        return jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ?", String.class, table);
    }

    private String indexDefinition(String index) {
        return jdbcTemplate.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = ?", String.class, index);
    }
}
