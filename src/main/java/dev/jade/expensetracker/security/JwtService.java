package dev.jade.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;


/**
 * Central service for creating, verifying, and reading JSON Web Tokens (JWTs).
 *
 * <p>This service encapsulates JWT operations using the JJWT library. Tokens
 * are generated with a subject (usually the username), an issued timestamp,
 * an expiration timestamp, and a cryptographic signature based on a secret
 * key. Verification and parsing are done with the same signing key,
 * ensuring signed tokens are valid and unaltered.</p>
 *
 * <p>The core JJWT API for token parsing uses {@code JwtParserBuilder} from
 * the JJWT library to configure signature verification and then produces a
 * {@code JwtParser}, which parses signed JWTs via
 * {@code parseSignedClaims(String)}. See the official API documentation:
 * {@link io.jsonwebtoken.JwtParserBuilder} and related JJWT docs for
 * detailed method behavior.</p>
 *
 * <p>Example of parsing a signed JWT with verification (from JJWT docs):
 * <pre>
 * Jwts.parser()
 *     .verifyWith(secretKey)
 *     .build()
 *     .parseSignedClaims(compactJws)
 *     .getPayload();
 * </pre>
 * This sequence verifies the signature using the configured key and then
 * parses the claims. If verification fails, a {@code JwtException} is thrown. :contentReference[oaicite:0]{index=0}</p>
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    /**
     * Derives a {@code SecretKey} suitable for signing and verifying JWTs.
     * <p>The configured secret is base64-encoded in properties. This method
     * decodes it and creates an HMAC SHA key instance that JJWT expects for
     * signature operations.</p>
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT string for the given user details.
     * <p>The token includes:
     * - the user’s username as subject,
     * - the current timestamp as issuedAt,
     * - an expiration timestamp based on configuration,
     * - a cryptographic signature using the signing key</p>
     *
     * @param userDetails the user principal for whom the token is issued
     * @return a compact signed JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Checks if the token’s expiration date has already passed.
     *
     * @param token the JWT string
     * @return true if expired; false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts a specific claim value from the JWT.
     * <p>This method parses all claims via the configured parser and
     * applies a resolver function to retrieve the desired claim.</p>
     *
     * @param token          the JWT string
     * @param claimsResolver a function extracting data from the claims set
     * @param <T>            type of the returned claim value
     * @return the specific claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT string and retrieves all claims after verifying the
     * signature with the configured secret key.
     * <p>Under the hood this uses a {@code JwtParserBuilder} with
     * {@code verifyWith(SecretKey)} then {@code build()} to produce a
     * {@code JwtParser}, and finally {@code parseSignedClaims(token)} to
     * both verify signature and extract the payload.</p>
     *
     * @param token the signed JWT string
     * @return the parsed {@code Claims} payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the username (subject) from the token.
     *
     * @param token the JWT string
     * @return the subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the token claims.
     *
     * @param token the JWT string
     * @return the expiration {@code Date}
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validates that the token’s subject matches the provided user details
     * and that the token has not expired.
     *
     * @param token       the JWT string
     * @param userDetails the expected user principal
     * @return true if token is valid; false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
