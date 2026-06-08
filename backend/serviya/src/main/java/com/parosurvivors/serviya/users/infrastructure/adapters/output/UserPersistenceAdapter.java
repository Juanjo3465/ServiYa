package com.parosurvivors.serviya.users.infrastructure.adapters.output;

import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import com.parosurvivors.serviya.users.domain.User;
import com.parosurvivors.serviya.users.infrastructure.entities.UserEntity;
import com.parosurvivors.serviya.users.infrastructure.mappers.UserPersistenceMapper;
import com.parosurvivors.serviya.users.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository repository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserEntity saved = repository.save(mapper.toEntity(user));
        return mapper.toDomain(saved);
    }

    @Override
    public User update(User user) {
        UserEntity updated = repository.save(mapper.toEntity(user));
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
