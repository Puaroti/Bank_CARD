package com.example.bankcards.repository;

import com.example.bankcards.entity.OperationHistoryEntity;
import com.example.bankcards.entity.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для истории операций по картам.
 */
public interface OperationHistoryRepository extends JpaRepository<OperationHistoryEntity, Long> {
    /**
     * Возвращает историю операций по конкретной карте.
     * @param cardId идентификатор карты
     * @param pageable параметры пагинации
     * @return страница записей истории
     */
    Page<OperationHistoryEntity> findAllByCard_Id(Long cardId, Pageable pageable);
    /**
     * Возвращает историю операций по всем картам пользователя.
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница записей истории
     */
    Page<OperationHistoryEntity> findAllByCard_User_Id(Long userId, Pageable pageable);
    /**
     * Возвращает историю операций по типу операции.
     * @param type тип операции
     * @param pageable параметры пагинации
     * @return страница записей истории
     */
    Page<OperationHistoryEntity> findAllByOperationType(OperationType type, Pageable pageable);
}
