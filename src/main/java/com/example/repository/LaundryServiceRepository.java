package com.example.repository;

import com.example.domain.LaundryService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaundryServiceRepository extends JpaRepository<LaundryService, Long> {
    Optional<LaundryService> findByName(String name);
    boolean existsByName(String name);
    List<LaundryService> findByActiveTrueOrderByNameAsc();

    @Query("SELECT s FROM LaundryService s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.name ASC")
    List<LaundryService> searchByName(@Param("query") String query);

    List<LaundryService> findAllByOrderByNameAsc();
}
