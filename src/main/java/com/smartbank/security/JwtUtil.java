package com.smartbank.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final Key key;
    private final long jwtExpirationMs;

    public JwtUtil(org.springframework.core.env.Environment env) {
        // read secret from properties
        String secret = env.getProperty("security.jwt.secret", "0qGA8d/cASWpDzyCihc1JXGF2FaxlokECig1Sg2uyjQ=");
        this.jwtExpirationMs = Long.parseLong(env.getProperty("security.jwt.expiration-ms","3600000")); // default 1h
        // key must be long enough
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, Set<String> roles) {
        long now = System.currentTimeMillis();
        String rolesStr = roles.stream().collect(Collectors.joining(","));
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", rolesStr)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // log if you want
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public Set<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        String roles = claims.get("roles", String.class);
        if (roles == null || roles.isEmpty()) return Set.of();
        return Set.of(roles.split(","));
    }
}
/*
1.Header:
{
  "alg": "HS256",              // algorithm
  "typ": "JWT"                 // token type
}

2.Body of Request:
{
  "sub": "theUsername",        // setSubject
  "roles": "ROLE_USER,ROLE_ADMIN", // your custom claim
  "iat": 1697145600,           // timestamp when issued
  "exp": 1697152800            // expiration timestamp
}
3.signWith(key, SignatureAlgorithm.HS256)
 */
