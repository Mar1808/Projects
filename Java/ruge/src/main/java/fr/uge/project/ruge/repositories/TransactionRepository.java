package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.TransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
  boolean existsByProviderAndProviderPaymentId(String provider, String providerPaymentId);
  long countByTransactionType(String transactionType);
  List<TransactionEntity> findAllByOrderByDateDesc(Pageable pageable);
  List<TransactionEntity> findByTransactionTypeOrderByDateDesc(String transactionType, Pageable pageable);
  List<TransactionEntity> findByTransactionTypeAndTargetRecipe_Creator_IdOrderByDateDesc(
      String transactionType,
      Long creatorId,
      Pageable pageable
  );

  @Query("SELECT t FROM TransactionEntity t WHERE " +
      "(t.date >= :startDate) AND " +
      "(t.date <= :endDate) AND " +
      "(:walletSourceId = -1L OR t.sourceWallet.id = :walletSourceId) AND" +
      "(:walletDestinationId = -1L OR t.destinationWallet.id = :walletDestinationId) " +
      "ORDER BY t.date DESC")
  List<TransactionEntity> findWithFilters(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("walletSourceId") Long walletSourceId,
      @Param("walletDestinationId") Long walletDestinationId,
      Pageable pageable
  );

  @Query("""
      SELECT t
      FROM TransactionEntity t
      WHERE t.referenceId IN :superLikeIds
      AND t.destinationWallet.type = :distributionWalletType
      AND t.sourceWallet.type = :userWalletType
      AND (
        t.transactionType = :reserveType
        OR (
          t.transactionType IS NULL
          AND LOWER(COALESCE(t.description, '')) LIKE '%redistribution%'
        )
      )
      """)
  List<TransactionEntity> findReserveTransactionsForSuperLikeIds(@Param("superLikeIds") Collection<Long> superLikeIds,
                                                                 @Param("distributionWalletType") String distributionWalletType,
                                                                 @Param("userWalletType") String userWalletType,
                                                                 @Param("reserveType") String reserveType);
}
