package com.sanskaracademy.job;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.sanskaracademy.service.StaffHrService;

/** Every morning 08:30 (India time): staff birthday wishes and festival greetings for holidays marked "send wishes". */
@Configuration
@EnableScheduling
public class StaffWishJob {

    private final StaffHrService hr;

    public StaffWishJob(StaffHrService hr) {
        this.hr = hr;
    }

    @Scheduled(cron = "0 30 8 * * *", zone = "Asia/Kolkata")
    public void morning() {
        hr.sendTodaysWishes(LocalDate.now(ZoneId.of("Asia/Kolkata")));
    }
}
