

package com.example.bankcards.repository;

import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<CardEntity, Long> {
    /**
     * Ищет карту по зашифрованному номеру.
     *
     * @param cardNumberEncrypted зашифрованный номер карты
     * @return Optional с найденной картой
     */
    Optional<CardEntity> findByCardNumberEncrypted(String cardNumberEncrypted);

    @Query("select c from CardEntity c where c.user.id = :userId")
    /**
     * Возвращает все карты пользователя с пагинацией.
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с картами
     */
    Page<CardEntity> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("select c from CardEntity c where c.user.id = :userId and (:status is null or c.status = :status) and (:owner is null or lower(c.owner) like lower(concat('%', :owner, '%'))) and (:q is null or (lower(c.owner) like lower(concat('%', :q, '%'))))")
    /**
     * Ищет карты пользователя по фильтрам: статус, владелец и строка поиска.
     *
     * @param userId идентификатор пользователя
     * @param status статус карты (необязательно)
     * @param owner подстрока имени владельца (необязательно)
     * @param query произвольная строка поиска (необязательно)
     * @param pageable параметры пагинации
     * @return страница с результатами поиска
     */
    Page<CardEntity> search(@Param("userId") Long userId,
                            @Param("status") CardStatus status,
                            @Param("owner") String owner,
                            @Param("q") String query,
                            Pageable pageable);

    @Query("select c from CardEntity c where (:status is null or c.status = :status) and (:owner is null or lower(c.owner) like lower(concat('%', :owner, '%'))) and (:q is null or (lower(c.owner) like lower(concat('%', :q, '%'))))")
    /**
     * Ищет карты по всему пулу (без ограничения по пользователю) — только для админов.
     */
    Page<CardEntity> searchAll(@Param("status") CardStatus status,
                               @Param("owner") String owner,
                               @Param("q") String query,
                               Pageable pageable);

    @Query("select c from CardEntity c where c.user.id = :userId and c.status = 'ACTIVE' and c.cardNumberEncrypted in :encryptedNumbers")
    /**
     * Возвращает активные карты пользователя по списку зашифрованных номеров.
     *
     * @param userId идентификатор пользователя
     * @param encryptedNumbers список зашифрованных номеров карт
     * @return список активных карт
     */
    List<CardEntity> findActiveByUserAndEncryptedNumbers(@Param("userId") Long userId,
                                                         @Param("encryptedNumbers") List<String> encryptedNumbers);
}
