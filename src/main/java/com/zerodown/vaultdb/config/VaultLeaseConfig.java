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
import org.springframework.vault.core.lease.event.AfterSecretLeaseRenewedEvent;
import org.springframework.vault.core.lease.event.LeaseListenerAdapter;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;
import org.springframework.vault.support.LeaseStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class VaultLeaseConfig {
    @Value("${spring.cloud.vault.database.role}")
    private String databaseRole;

    private final ApplicationContext applicationContext;

    @Autowired
    private VaultOperations operations;


    SecretLeaseContainer leaseContainer;


    @Autowired
    private void initSecretLeaseContainer(SecretLeaseContainer leaseContainer) {
        leaseContainer.setLeaseStrategy(LeaseStrategy.dropOnError());
        this.leaseContainer = leaseContainer;
    }

    private ConcurrentHashMap<String, SecretLeaseEvent> requestedLeaseIds = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        final String vaultCredsPath = String.format("database/creds/%s", databaseRole);

        //leaseContainer = new SecretLeaseContainer(operations);
        leaseContainer.addErrorListener(new LeaseListenerAdapter() {
            @Override
            public void onLeaseError(SecretLeaseEvent leaseEvent, Exception exception) {
                log.info("==> OnLeaseErrorHandler HANDLE event: event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());
                //

                if(leaseEvent.getSource().getPath().equals(vaultCredsPath) && leaseEvent.getSource().getMode() == RequestedSecret.Mode.ROTATE) {
                    //get a new one instead.
                    leaseContainer.rotate(RequestedSecret.rotating(leaseEvent.getSource().getPath()));
                    //leaseContainer.requestRotatingSecret(leaseEvent.getSource().getPath());
                }
                if(leaseEvent.getSource().getPath().equals(vaultCredsPath) && leaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                    //get a new one instead.
                    //leaseContainer.rotate(RequestedSecret.rotating(leaseEvent.getSource().getPath()));
                    leaseContainer.requestRotatingSecret(leaseEvent.getSource().getPath());
                }
            }
        });
        leaseContainer.addLeaseListener(new LeaseListenerAdapter() {
            @Override
            public void onLeaseEvent(SecretLeaseEvent leaseEvent) {
                log.info("==>STARTING Received event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());

                if (vaultCredsPath.equals(leaseEvent.getSource().getPath())) {
                    if (leaseEvent instanceof SecretLeaseExpiredEvent &&
                            leaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                        log.info("==> Add a ROTATE secret instead of the current RENEW lease by event: {}, leaseId: {}", leaseEvent, leaseEvent.getLease().getLeaseId());
                        leaseContainer.requestRotatingSecret(vaultCredsPath);
                    } else if (leaseEvent instanceof AfterSecretLeaseRenewedEvent
                            && leaseEvent.getSource().getMode() == RequestedSecret.Mode.ROTATE
                            && leaseEvent.getSource().getPath().equals(vaultCredsPath) && !requestedLeaseIds.containsKey(leaseEvent.getLease().getLeaseId())) {
                        leaseContainer.rotate(RequestedSecret.rotating(leaseEvent.getSource().getPath()));
                        requestedLeaseIds.put(leaseEvent.getLease().getLeaseId(), leaseEvent);
                        log.info("==>Forced Early Rotation, AfterSecretLeaseRenewed event: {}, leaseId: {}\", leaseEvent, leaseEvent.getLease().getLeaseId());");
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
                        //requestedLeaseIds.put(secretLeaseCreatedEvent.getLease().getLeaseId(), leaseEvent);
                    } else if (System.currentTimeMillis() > (leaseEvent.getTimestamp() + leaseEvent.getLease().getLeaseDuration().toMillis())) {
                        log.info("Current TimeMills {}, leaseEventTimestamp: {}, leaseEventDurationMillis: {}, system - (event-duration) {}",
                                System.currentTimeMillis(),
                                leaseEvent.getTimestamp(),
                                leaseEvent.getLease().getLeaseDuration().toMillis(),
                                System.currentTimeMillis() - (leaseEvent.getTimestamp() + leaseEvent.getLease().getLeaseDuration().toMillis()));
                        //something strange has happened, the lease has expired, without it being a leaseexpiredevent.
                        log.error("==> ALERT!!! leaseTimestamp < currentTimeMillis, meaning it should have already expired!!!: ID: {}, path:{}, mode:{}", leaseEvent.getLease().getLeaseId(), leaseEvent.getSource().getPath(),leaseEvent.getSource().getMode().toString());

                    }
                    for (Map.Entry<String, SecretLeaseEvent> secretLeaseEvent : requestedLeaseIds.entrySet()) {
                        if(System.currentTimeMillis() > (secretLeaseEvent.getValue().getTimestamp() + secretLeaseEvent.getValue().getLease().getLeaseDuration().toMillis())) {
                            log.info("Current TimeMills {}, leaseEventTimestamp: {}, leaseEventDurationMillis: {}, system - (event-duration) {}",
                                    System.currentTimeMillis(),
                                    secretLeaseEvent.getValue().getTimestamp(),
                                    secretLeaseEvent.getValue().getLease().getLeaseDuration().toMillis(),
                                    System.currentTimeMillis() - (secretLeaseEvent.getValue().getTimestamp() + secretLeaseEvent.getValue().getLease().getLeaseDuration().toMillis()));
                            requestedLeaseIds.remove(secretLeaseEvent.getKey());
                        }
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