package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.RefreshToken;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.exception.TokenRequestInvalidException;
import congtuong.dev.cinemabooking.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import redis.clients.authentication.core.TokenRequestException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final TokenHashService tokenHashService;

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

        if (token.getExpiration().isBefore(Instant.now())) {
            throw new TokenRequestInvalidException("Refresh token is expired");
        }

        return token;
    }

    @Override
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
        refreshTokenRepository.save(token);
    }
}
