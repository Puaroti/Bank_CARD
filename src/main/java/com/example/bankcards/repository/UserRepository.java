package com.example.bankcards.repository;

import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.dto.UserWithCardCountDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий пользователей.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * Находит пользователя по имени.
     * @param username имя пользователя
     * @return Optional с найденным пользователем
     */
    Optional<UserEntity> findByUsername(String username);
    /**
     * Проверяет существование пользователя по имени.
     * @param username имя пользователя
     * @return true, если пользователь существует
     */
    boolean existsByUsername(String username);

    /**
     * Возвращает всех пользователей с количеством их карт.
     */
    @Query("select new com.example.bankcards.dto.UserWithCardCountDto(u.id, u.username, u.fullName, u.firstName, u.lastName, u.patronymic, u.role, count(c)) " +
           "from UserEntity u left join u.cards c group by u.id, u.username, u.fullName, u.firstName, u.lastName, u.patronymic, u.role")
    java.util.List<UserWithCardCountDto> findAllWithCardCounts();
}
