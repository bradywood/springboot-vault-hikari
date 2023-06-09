package com.zerodown.vaultdb.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.vault.config.databases.VaultDatabaseProperties;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;

@Slf4j
//@Configuration
//@ConditionalOnClass(SecretLeaseContainer.class)
//@RequiredArgsConstructor
public class VConfig implements InitializingBean {

    private  SecretLeaseContainer container;
    private  HikariDataSource hikariDataSource;
    private  VaultDatabaseProperties properties;

    //@Value("${spring.cloud.vault.database.role}")
    private String databaseRole;

    @Override
    public void afterPropertiesSet() throws Exception {
        String vaultCredsPath = "database/creds/$databaseRole";
        var secret = RequestedSecret.rotating(vaultCredsPath);

        log.info("", "");

        container.addLeaseListener(leaseEvent -> {
            log.info("Vault: Lease Event: {}", leaseEvent);
            if (leaseEvent.getSource() == secret && leaseEvent instanceof SecretLeaseCreatedEvent) {
                log.info("Vault: Refreshing database credentials. Lease Event: {}", leaseEvent);
                var lease = (SecretLeaseCreatedEvent) leaseEvent;
                var username = lease.getSecrets().get("username").toString();
                var password = lease.getSecrets().get("password").toString();

                hikariDataSource.setUsername(username);
                hikariDataSource.setPassword(password);
                hikariDataSource.getHikariConfigMXBean().setUsername(username);
                hikariDataSource.getHikariConfigMXBean().setPassword(password);
            }
        });

        container.addRequestedSecret(secret);
    }
}

