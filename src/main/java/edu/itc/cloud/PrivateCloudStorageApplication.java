package edu.itc.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 08 — Private Cloud Storage App.
 *
 * <p>A small Spring Boot backend where every registered user manages files and
 * folders inside their <strong>own</strong> isolated storage. New accounts are
 * granted a 50&nbsp;MB quota; a user can update their profile and delete their
 * own account together with all of their data.</p>
 *
 * <p>The accompanying test suite (see {@code src/test/java}) demonstrates all
 * ten testing methods from Lesson 08 and reports through Allure.</p>
 */
@SpringBootApplication
public class PrivateCloudStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrivateCloudStorageApplication.class, args);
    }
}
