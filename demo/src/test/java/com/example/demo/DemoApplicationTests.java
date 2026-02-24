// Делал тесты на базе статьи https://habr.com/ru/articles/561520/

package com.example.demo;

import com.example.demo.model.BirthdayModel;
import com.example.demo.repository.BirthdayRepository;
import com.example.demo.service.BirthdayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

    @Autowired
    BirthdayRepository birthdayRepository;

    @Autowired
    BirthdayService birthdayService;

    @BeforeEach
    void clean() {
        birthdayRepository.deleteAll();
        System.out.println("=== Baza jchishena ===");
    }

    @Test
    void contextLoads() {
        System.out.println("=== Proverka zapuska kontexta ===");
        assertThat(birthdayService).isNotNull();
        System.out.println("Kontext podnyat!");
    }

    @Test
    void addAndGetAllShouldWork() {
        System.out.println("=== TEST dobavlenie i poluchenie zapisey ===");

        BirthdayModel b = new BirthdayModel();
        b.setName("Test");
        b.setBirthdayDate(LocalDate.of(2003, 3, 28));

        birthdayService.addBirthday(b);
        System.out.println("Dobavlena zapis: Test");

        List<BirthdayModel> all = birthdayService.getAllBirthdays();
        System.out.println("Kol-vo zapisey v BD: " + all.size());

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getName()).isEqualTo("Test");

        System.out.println("TEST addAndGetAllShouldWork vipolnen");
    }

    @Test
    void upcomingShouldReturnBirthdaysInRange() {
        System.out.println("=== Test blijaishih dr ===");

        BirthdayModel inRange = new BirthdayModel();
        inRange.setName("InRange");
        inRange.setBirthdayDate(LocalDate.now().minusYears(20).plusDays(3));

        BirthdayModel outOfRange = new BirthdayModel();
        outOfRange.setName("OutRange");
        outOfRange.setBirthdayDate(LocalDate.now().minusYears(20).plusDays(20));

        birthdayService.addBirthday(inRange);
        birthdayService.addBirthday(outOfRange);

        List<BirthdayModel> upcoming = birthdayService.getUpcomingBirthdays(7);

        System.out.println("Naydeno blijaishih DR: " + upcoming.size());
        upcoming.forEach(b -> 
            System.out.println(" - " + b.getName())
        );

        assertThat(upcoming).extracting(BirthdayModel::getName)
                .contains("InRange")
                .doesNotContain("OutRange");

        System.out.println("TEST upcomingShouldReturnBirthdaysInRange vipolnen");
    }
}