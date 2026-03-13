package com.API_Gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final String secret;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
//Component:This tells Spring Boot to automatically
// create and manage this class as a Bean so it can be injected anywhere in the application.

//Secret Key:This is the secret key used to verify the JWT signature.
//When a token is created, it is signed with this secret key,
// and while validating we use the same key to verify that the token is genuine.

//public Claims validateToken(String token)
//Takes a JWT token as input
//Validates the token
//Returns the claims (data inside the token)

//This class is a utility component used to validate JWT tokens in the API Gateway.
//It uses the JJWT library to parse the token and verify its signature using a secret key.
//If the token is valid, it extracts and returns the claims, which contain user information like username and roles.
//If the token is invalid or expired, an exception is thrown.