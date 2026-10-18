package com.auction.application.command;

import com.auction.domain.shared.Guard;
import com.auction.domain.user.User;
import com.auction.domain.user.UserId;
import com.auction.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.Optional;

/**
 * Mirrors an identity-provider user locally: registers it on first sight, updates it when its profile changed.
 */
@RequiredArgsConstructor
public class ProvisionUserUseCase {

    private final UserRepository userRepository;
    private final Clock clock;

    public UserId handle(ProvisionUserCommand command) {
        Guard.notNull(command, "command");
        Optional<User> existing = userRepository.findById(command.userId());
        if (existing.isEmpty()) {
            userRepository.save(User.register(command.userId(), command.fullName(), command.email(),
                    command.phone(), command.role(), clock));
        } else if (existing.get().updateProfile(command.fullName(), command.email(), command.phone(),
                command.role(), clock)) {
            userRepository.save(existing.get());
        }
        return command.userId();
    }
}
