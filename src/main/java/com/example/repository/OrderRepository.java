package com.example.repository;

import com.example.domain.Customer;
import com.example.domain.Order;
import com.example.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByInvoiceNumber(String invoiceNumber);

    List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);

    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);

    List<Order> findAllByOrderByOrderDateDesc();

    List<Order> findTop5ByOrderByOrderDateDesc();

    long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR o.customer.phone LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY o.orderDate DESC")
    List<Order> searchOrders(@Param("query") String query);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND " +
           "(LOWER(o.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR o.customer.phone LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY o.orderDate DESC")
    List<Order> searchOrdersByStatus(@Param("status") OrderStatus status, @Param("query") String query);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :start AND :end ORDER BY o.orderDate DESC")
    List<Order> findOrdersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.paidAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    BigDecimal sumPaidAmountInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    BigDecimal sumTotalAmountInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    boolean existsByCustomerId(Long customerId);
}
