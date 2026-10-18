package com.auction.infrastructure.persistence.user;

import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void mapsDomainToEntityAndBack() {
        User user = User.register("Jane Doe", new Email("jane@example.com"), new Phone("+33612345678"), Role.SELLER,
                Clock.fixed(Instant.parse("2026-01-10T10:00:00Z"), ZoneOffset.UTC));

        UserEntity entity = UserMapper.toEntity(user);
        User restored = UserMapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(user.getId().value());
        assertThat(entity.getEmail()).isEqualTo("jane@example.com");
        assertThat(entity.getPhone()).isEqualTo("+33612345678");
        assertThat(entity.isDeleted()).isFalse();
        assertThat(restored).usingRecursiveComparison().isEqualTo(user);
    }
}
