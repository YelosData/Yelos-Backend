
package com.yelos.backend;

import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.purchase.repo.PurchaseRepo;
import com.yelos.backend.transaction.repo.TransactionRepo;
import com.yelos.backend.request_marketplace.repo.DatasetRequestRepo;
import com.yelos.backend.request_marketplace.repo.RequestSubmissionRepo;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("yelos_test")
            .withUsername("yy")
            .withPassword("123456");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-32-characters-long");
        registry.add("jwt.access-expiration-ms", () -> "900000");
        registry.add("jwt.refresh-expiration-ms", () -> "604800000");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepo userRepo;

    @Autowired
    protected DataPostRepo dataPostRepo;

    @Autowired
    protected PurchaseRepo purchaseRepo;

    @Autowired
    protected TransactionRepo transactionRepo;

    @Autowired
    protected DatasetRequestRepo datasetRequestRepo;

    @Autowired
    protected RequestSubmissionRepo requestSubmissionRepo;

    @BeforeEach
    void cleanDatabase() {
        requestSubmissionRepo.deleteAll();
        datasetRequestRepo.deleteAll();
        transactionRepo.deleteAll();
        purchaseRepo.deleteAll();
        dataPostRepo.deleteAll();
        userRepo.deleteAll();
    }
}