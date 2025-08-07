package com.example.bankcards.repository;

import com.example.bankcards.entity.TransferEntity;
import com.example.bankcards.entity.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий переводов между картами.
 */
public interface TransferRepository extends JpaRepository<TransferEntity, Long> {
    /**
     * Возвращает переводы, где пользователь является отправителем или получателем.
     * @param fromUserId идентификатор пользователя-отправителя
     * @param toUserId идентификатор пользователя-получателя
     * @param pageable параметры пагинации
     * @return страница переводов
     */
    Page<TransferEntity> findAllByFromCard_User_IdOrToCard_User_Id(Long fromUserId, Long toUserId, Pageable pageable);
    /**
     * Возвращает переводы по статусу.
     * @param status статус перевода
     * @param pageable параметры пагинации
     * @return страница переводов
     */
    Page<TransferEntity> findAllByStatus(TransferStatus status, Pageable pageable);
}
