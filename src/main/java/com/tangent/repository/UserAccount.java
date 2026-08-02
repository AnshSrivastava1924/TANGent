package com.tangent.repository;

public record UserAccount(long id, String email, String passwordHash, String fullName) {
}
