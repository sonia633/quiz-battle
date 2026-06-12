package com.quizbattle.quiz.entity;

/**
 * Application roles. Stored as a string in the {@code roles} table and surfaced
 * as Spring Security authorities prefixed with {@code ROLE_}.
 */
public enum RoleName {
    PLAYER,
    MODERATOR,
    ADMINISTRATOR
}
