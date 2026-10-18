package com.auction.infrastructure.persistence.user;

import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    @Transactional
    public User save(User user) {
        return UserMapper.toDomain(jpaRepository.save(UserMapper.toEntity(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(UserMapper::toDomain);
    }
}
