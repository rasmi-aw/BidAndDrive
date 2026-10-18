package com.auction.infrastructure.persistence.user;

import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserRepository;
import com.auction.infrastructure.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.auction.infrastructure.PersistenceFixtures.newSeller;
import static org.assertj.core.api.Assertions.assertThat;

class UserPersistenceAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndLoadsAUser() {
        User user = newSeller();

        userRepository.save(user);

        assertThat(userRepository.findById(user.getId()))
                .get().usingRecursiveComparison().isEqualTo(user);
    }

    @Test
    void unknownUserIsEmpty() {
        assertThat(userRepository.findById(UserId.newId())).isEmpty();
    }

    @Test
    void softDeletedUserIsNotFound() {
        User user = userRepository.save(newSeller());

        jdbcTemplate.update("UPDATE users SET deleted = TRUE WHERE id = ?", user.getId().value());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isEqualTo(1);
    }
}
