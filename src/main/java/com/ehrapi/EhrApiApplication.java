package com.ehrapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the modular Electronic Health Record (EHR) API.
 *
 * <p>The platform is organised as a catalog of independent clinical modules
 * (demographics, encounters, problems, medications, allergies, vitals, ...).
 * Each institution enables only the modules that suit its clinical needs, and
 * patients can consent to share their record across institutions.
 */
@SpringBootApplication
public class EhrApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(EhrApiApplication.class, args);
    }
}
