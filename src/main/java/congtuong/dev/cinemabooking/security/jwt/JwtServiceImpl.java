package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.enums.Role;
import congtuong.dev.cinemabooking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.accessTokenExpiration());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("sv", user.getSecurityVersion())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(this.secretKey)
                .compact();
    }

    @Override
    public Claims parseToken(String token) {
        JwtParserBuilder parser = Jwts.parser();
        parser.verifyWith(secretKey);
        JwtParser jwtParser = parser.build();
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims;
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    @Override
    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    @Override
    public Instant extractExpiration(String token) {
        return parseToken(token)
                .getExpiration()
                .toInstant();
    }


    @Override
    public Role extractRole(String token) {
        String role = parseToken(token).get("role", String.class);
        return Role.valueOf(role);
    }

    @Override
    public long extractSecurityVersion(String token) {
        Number version = parseToken(token).get("sv", Number.class);
        if (version == null) {
            throw new IllegalArgumentException("Token has no security version");
        }
        return version.longValue();
    }
}
