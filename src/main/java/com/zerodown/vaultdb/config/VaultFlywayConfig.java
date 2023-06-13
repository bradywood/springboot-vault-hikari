package com.zerodown.vaultdb.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.SecretLeaseContainer;

import javax.sql.DataSource;

@Slf4j
@Configuration
@ConditionalOnClass(VaultOperations.class)
public class VaultFlywayConfig {

    @Value("${spring.application.db.flyway.role}")
    private String flywayRole;

    @Value("${spring.datasource.url}")
    private String url;

    @Autowired
    private VaultOperations vaultOperations;

    @Autowired
    private VaultLeaseConfig vaultLeaseConfig;

    @Autowired
    SecretLeaseContainer leaseContainer;

    @Autowired
    DataSource dataSource;

    @Bean
    FlywayConfigurationCustomizer flywayVaultConfiguration() {

        FlywayConfigurationCustomizer flywayConfigurationCustomizer = new FlywayConfigurationCustomizer() {
            @Override
            public void customize(org.flywaydb.core.api.configuration.FluentConfiguration configuration) {
                configuration
                        //.dataSource(url, username, password).
                        .dataSource(dataSource);

                /*String vaultCredsPath = String.format("database/creds/%s", flywayRole);
                vaultCredsPath = "database/creds/readonly";
                VaultResponse vaultResponse = vaultOperations.read(vaultCredsPath);
                if (null != vaultResponse) {
                    String username = vaultResponse.getData().get("username").toString();
                    String password = vaultResponse.getData().get("password").toString();
                    configuration
                            //.dataSource(url, username, password).
                                    .dataSource(dataSource);
                            //.initSql(String.format("SET ROLE %s",flywayRole));
                            //.initSql(String.format("SET ROLE " + vaultCredsPath));
                    log.info("Vault: Flyway configured with credentials from Vault. Credential path: {}", vaultCredsPath);
                }*/
            }
        };

        return flywayConfigurationCustomizer;
    }
}