package com.zerodown.vaultdb.config;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.LeaseListenerAdapter;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class VaultLeaseConfig {
    @Value("${spring.cloud.vault.database.role}")
    private String databaseRole;

    private final ApplicationContext applicationContext;

    @Autowired
    private VaultOperations operations;

    @Autowired
    SecretLeaseContainer leaseContainer;

    @PostConstruct
    private void postConstruct() {
        final String vaultCredsPath = String.format("database/creds/%s", databaseRole);

        //leaseContainer = new SecretLeaseContainer(operations);
        leaseContainer.addErrorListener(new LeaseListenerAdapter() {
            @Override
            public void onLeaseError(SecretLeaseEvent leaseEvent, Exception exception) {
                log.info("==> OnLeaseErrorHandler HANDLE event: event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());
                leaseContainer.requestRotatingSecret(leaseEvent.getSource().getPath());
            }
        });
        leaseContainer.addLeaseListener(new LeaseListenerAdapter() {
            @Override
            public void onLeaseEvent(SecretLeaseEvent leaseEvent) {
                log.info("==>STARTING Received event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());

                if (vaultCredsPath.equals(leaseEvent.getSource().getPath())) {
                    if (leaseEvent instanceof SecretLeaseExpiredEvent &&
                            leaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                        log.info("==> Replace RENEW lease by a ROTATE one. event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());
                        leaseContainer.requestRotatingSecret(vaultCredsPath);
                    } else if (leaseEvent instanceof SecretLeaseCreatedEvent && leaseEvent.getSource().getMode() == RequestedSecret.Mode.ROTATE) {
                        SecretLeaseCreatedEvent secretLeaseCreatedEvent = (SecretLeaseCreatedEvent) leaseEvent;
                        String username = (String) secretLeaseCreatedEvent.getSecrets().get("username");
                        String password = (String) secretLeaseCreatedEvent.getSecrets().get("password");

                        log.info("==> Update System properties username & password");
                        System.setProperty("spring.datasource.username", username);
                        System.setProperty("spring.datasource.password", password);

                        log.info("==> spring.datasource.username: {}", username);

                        updateDataSource(username, password);
                        log.info("==> DONE updateDataSource");
                    }

                    log.info("==> DONE HANDLE event: event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());
                }
            }
        });

        leaseContainer.start();
        System.out.println("Whatsup.");
    }

    private synchronized void updateDataSource(String username, String password) {
        HikariDataSource hikariDataSource = (HikariDataSource) applicationContext.getBean("dataSource");

        log.info("==> Update database credentials");
        HikariConfigMXBean hikariConfigMXBean = hikariDataSource.getHikariConfigMXBean();

        hikariConfigMXBean.setUsername(username);
        hikariConfigMXBean.setPassword(password);

        //we dont need to evict database connections, this can happen automatically on failure?
        log.info("==> Do not Soft evict database connections.");
        HikariPoolMXBean hikariPoolMXBean = hikariDataSource.getHikariPoolMXBean();
        if (hikariPoolMXBean != null) {
            hikariPoolMXBean.softEvictConnections();
        }

    }

}