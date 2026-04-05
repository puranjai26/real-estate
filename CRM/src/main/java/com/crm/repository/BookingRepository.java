package com.crm.repository;

import com.crm.entity.Booking;
import com.crm.entity.Property;
import com.crm.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    boolean existsByPropertyAndStatus(Property property, BookingStatus status);
    List<Booking> findByProperty(Property property);
    long countByStatus(BookingStatus status);
}
