package com.example.demo.repository;

import com.example.demo.model.BirthdayModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BirthdayRepository extends JpaRepository<BirthdayModel, Integer>{
    
    List<BirthdayModel> findByBirthdayDate(LocalDate birthdayDate);

    List<BirthdayModel> findByBirthdayDateBetween(LocalDate startDate, LocalDate endDate);

    List<BirthdayModel> findByBirthdayDateAfter(LocalDate date);

}
