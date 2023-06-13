package com.zerodown.vaultdb.repository;

import com.zaxxer.hikari.HikariDataSource;
import com.zerodown.vaultdb.stats.CompletedCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CustomerRepositoryWorker implements Callable<String> {


    private final ApplicationContext applicationContext;
    private final CompletedCounter completedCounter;

    private static final int RETRY_MAX = 5;
    private AtomicInteger counter = new AtomicInteger(RETRY_MAX);

    public CustomerRepositoryWorker(ApplicationContext applicationContext, CompletedCounter completedCounter) {
        //this.dataSource = dataSource;
        this.applicationContext = applicationContext;
        this.completedCounter = completedCounter;
    }

    @Override
    public String call() {
        Connection connection = null;
        try {
            connection = getDatasource().getConnection();
            try (ResultSet rs = connection.createStatement().executeQuery("Select * from customer")) {
                if (rs.next()) {
                    log.info("Name {} Address {} RetryCounter {}", rs.getString("name"), rs.getString("address"), RETRY_MAX - counter.get());
                }
            }

            //hold on to for a second
            sleeper(1);
            completedCounter.atomicSuccessInteger.incrementAndGet();
            

        } catch (
                SQLException sqlException) {
            if (counter.decrementAndGet() > 0) {
                sleeper(5);
                return call();
            } else {
                completedCounter.atomicFailedInteger.incrementAndGet();
            }
        }
        finally {
            try {
                if (connection !=null) {
                    connection.close();
                }
            }
            catch (Exception ignore) {}
        }

        return "";
    }

    private void sleeper(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (Exception ignore) {}
    }

    private HikariDataSource getDatasource() {
        HikariDataSource hikariDataSource = (HikariDataSource) applicationContext.getBean("dataSource");
        /*int totalConnections = hikariDataSource.getHikariPoolMXBean().getTotalConnections();
        int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
        int freeConnections = totalConnections - activeConnections;
        int connectionWaiting = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
        log.info(String.format("number of connections in use by the application (active): %d.", activeConnections));
        log.info(String.format("the number of established but idle connections: %d.",  freeConnections));
        log.info(String.format("number of threads waiting for a connection: %d.",  connectionWaiting));
        log.info(String.format("max pool size: %d.", hikariDataSource.getMaximumPoolSize()));*/
        return hikariDataSource;
    }
}
