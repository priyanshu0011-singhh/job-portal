package com.ncst.job.portal.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenHelperTest {

    @Test
    void productionRejectsInvalidJwtSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        JwtTokenHelper helper = new JwtTokenHelper(environment);
        ReflectionTestUtils.setField(helper, "jwtSecret", "not-a-valid-production-secret");

        assertThrows(IllegalStateException.class,
            () -> helper.generateToken(new org.springframework.security.core.userdetails.User(
                "user@example.com", "password", java.util.List.of())));
    }

    @Test
    void productionAcceptsBase64SecretWithAtLeast64Bytes() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        JwtTokenHelper helper = new JwtTokenHelper(environment);
        ReflectionTestUtils.setField(helper, "jwtSecret", Base64.getEncoder().encodeToString(new byte[64]));

        assertFalse(helper.generateToken(new org.springframework.security.core.userdetails.User(
                "user@example.com", "password", java.util.List.of())).isBlank());
    }

    @Test
    void renderAcceptsStrongGeneratedSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("render");
        JwtTokenHelper helper = new JwtTokenHelper(environment);
        ReflectionTestUtils.setField(helper, "jwtSecret",
                Base64.getEncoder().encodeToString("a-strong-generated-render-secret-with-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertDoesNotThrow(() -> helper.generateToken(new org.springframework.security.core.userdetails.User(
                "user@example.com", "password", java.util.List.of())));
    }

    @Test
    void renderRejectsShortGeneratedSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("render");
        JwtTokenHelper helper = new JwtTokenHelper(environment);
        ReflectionTestUtils.setField(helper, "jwtSecret", "too-short");

        assertThrows(IllegalStateException.class,
                () -> helper.generateToken(new org.springframework.security.core.userdetails.User(
                        "user@example.com", "password", java.util.List.of())));
    }
}