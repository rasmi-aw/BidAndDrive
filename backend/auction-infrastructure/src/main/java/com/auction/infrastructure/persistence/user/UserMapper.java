package com.auction.infrastructure.persistence.user;

import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;

final class UserMapper {

    private UserMapper() {
    }

    static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity(user.getId().value());
        entity.setFullName(user.getFullName());
        entity.setEmail(user.getEmail().value());
        entity.setPhone(user.getPhone().value());
        entity.setRole(user.getRole());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }

    static User toDomain(UserEntity entity) {
        return User.restore(new UserId(entity.getId()), entity.getFullName(), new Email(entity.getEmail()),
                new Phone(entity.getPhone()), entity.getRole(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
