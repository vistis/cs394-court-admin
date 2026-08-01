package kh.edu.paragoniu.court_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "kh.edu.paragoniu.court_admin",
    "kh.edu.paragoniu.court_shared"
})
@EnableJpaRepositories(basePackages = {
    "kh.edu.paragoniu.court_shared.repository",
    "kh.edu.paragoniu.court_admin.repository"
})
@EnableCaching
@EntityScan(basePackages = {
    "kh.edu.paragoniu.court_shared.entity",
    "kh.edu.paragoniu.court_admin.entity"
})
public class CourtAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourtAdminApplication.class, args);
    }
}
