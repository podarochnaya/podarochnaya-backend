package com.vk.itmo.podarochnaya.backend.user.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE u.email IN (:emails)
        """)
    List<UserEntity> getAllByEmails(@Param("emails") Collection<String> emails);

    List<UserEntity> findByIdIn(List<Long> ids);
}
