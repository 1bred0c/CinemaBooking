package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.RefreshToken;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.exception.TokenRequestInvalidException;
import congtuong.dev.cinemabooking.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final TokenHashService tokenHashService;
    private final SecurityStateService securityStateService;

    @Override
    public String generateRefreshToken(User user) {
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(tokenHashService.hash(refreshToken))
                        .user(user)
                        .expiration(Instant.now().plus(jwtProperties.refreshTokenExpiration()))
                        .revoked(false)
                        .build()
        );
        return refreshToken;

    }

    @Override
    public RefreshToken getValidRefreshToken(String refreshToken) {
        String hashToken = tokenHashService.hash(refreshToken);

        RefreshToken token = refreshTokenRepository
                .findByToken(hashToken)
                .orElseThrow(() ->
                        new TokenRequestInvalidException("Invalid refresh token")
                );

        if (token.isRevoked()) {
            throw new TokenRequestInvalidException("Refresh token is revoked");
        }

        if (!token.getExpiration().isAfter(Instant.now())) {
            throw new TokenRequestInvalidException("Refresh token is expired");
        }

        if (!token.getUser().isActive()) {
            throw new TokenRequestInvalidException("User account is inactive");
        }

        if (securityStateService.isTransitioning(token.getUser().getId())) {
            throw new TokenRequestInvalidException("Account security information is being updated");
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        String hashToken = tokenHashService.hash(refreshToken);
        RefreshToken token = refreshTokenRepository
                .findByToken(hashToken)
                .orElseThrow(() ->
                        new TokenRequestInvalidException("Invalid refresh token")
                );
        if (token.isRevoked()) {
            throw new TokenRequestInvalidException("Token is revoked");
        }
        token.setRevoked(true);
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public int deleteExpiredAndRevokedTokens() {
        return refreshTokenRepository.deleteRevokedOrExpired(Instant.now());
    }
}
