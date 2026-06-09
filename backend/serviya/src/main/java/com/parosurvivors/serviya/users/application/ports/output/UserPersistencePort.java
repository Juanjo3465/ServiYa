package com.parosurvivors.serviya.users.application.ports.output;

import com.parosurvivors.serviya.users.domain.User;

import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);
    User update(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteById(Long id);
}
