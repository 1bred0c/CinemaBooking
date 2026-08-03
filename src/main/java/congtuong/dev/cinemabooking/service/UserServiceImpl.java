package congtuong.dev.cinemabooking.service;


import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.exception.UserNotFoundException;
import congtuong.dev.cinemabooking.repository.UserRepository;
import congtuong.dev.cinemabooking.security.jwt.RefreshTokenService;
import congtuong.dev.cinemabooking.security.jwt.SecurityStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final SecurityStateService securityStateService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void deactivate(UUID userId) {
        User user = findUser(userId);
        if (!user.isActive()) {
            return;
        }
        user.setActive(false);
        securityStateService.advanceVersion(user);
        refreshTokenService.revokeAllForUser(userId);
    }

    @Override
    @Transactional
    public void activate(UUID userId) {
        User user = findUser(userId);
        if (user.isActive()) {
            return;
        }
        user.setActive(true);
        securityStateService.advanceVersion(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

}
