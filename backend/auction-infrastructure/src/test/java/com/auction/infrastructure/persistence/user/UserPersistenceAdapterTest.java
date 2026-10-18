package com.auction.infrastructure.persistence.user;

import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserRepository;
import com.auction.infrastructure.InfrastructureIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;

import static com.auction.infrastructure.PersistenceFixtures.CLOCK;
import static com.auction.infrastructure.PersistenceFixtures.newSeller;
import static org.assertj.core.api.Assertions.assertThat;

class UserPersistenceAdapterTest extends InfrastructureIntegrationTest {

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
    void savingAnExistingUserUpdatesItsProfile() {
        User user = userRepository.save(newSeller());
        User loaded = userRepository.findById(user.getId()).orElseThrow();
        loaded.updateProfile("Renamed", new Email("renamed@example.com"), new Phone("+33611111111"), Role.BUYER,
                Clock.offset(CLOCK, Duration.ofMinutes(5)));

        userRepository.save(loaded);

        assertThat(userRepository.findById(user.getId()))
                .get().usingRecursiveComparison().isEqualTo(loaded);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isEqualTo(1);
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
