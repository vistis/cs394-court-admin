package kh.edu.paragoniu.court_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(
    basePackages = {
        "kh.edu.paragoniu.court_shared.entity",
        "kh.edu.paragoniu.court_admin.entity",
    }
)
public class CourtAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourtAdminApplication.class, args);
    }
}
