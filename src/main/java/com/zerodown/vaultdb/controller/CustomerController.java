package com.zerodown.vaultdb.controller;

import com.zerodown.vaultdb.entity.Customer;
import com.zerodown.vaultdb.repository.CustomerRepositoryWorker;
import com.zerodown.vaultdb.repository.UserRepository;
import com.zerodown.vaultdb.stats.CompletedCounter;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@Slf4j
@RequestMapping("/api")
public class CustomerController {

    @Autowired
    private DataSource userDataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    CompletedCounter completedCounter;

    @Autowired
    Flyway flyway;

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getCustomers() {
        List<Customer> customerList = userRepository.findAll();

        return ResponseEntity.ok()
                //.contentType(MediaType.APPLICATION_STREAM_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(customerList);
    }

    @GetMapping("/testCustomers")
    public ResponseEntity<String> getTestCustomers() {

        for(int ii=0;ii<1000;ii++) {
            int finalIi = ii;
            new Thread(() -> {
                System.out.format("ii[%d], List[%s]", finalIi, userRepository.findAll());
            }).start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //List<Customer> customerList = userRepository.findAll();

        return ResponseEntity.ok()
                //.contentType(MediaType.APPLICATION_STREAM_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body("done");
    }

    @GetMapping("/testCustomersCustom")
    public ResponseEntity<String> getTestCustomersCustom() {
        ExecutorService executor = Executors.newFixedThreadPool(1000);
        Set<Callable<String>> callables = new HashSet<Callable<String>>();

        for(int ii=0;ii<2000;ii++) {
            int finalIi = ii;
            callables.add(new CustomerRepositoryWorker(applicationContext, completedCounter));
        }
        try {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            log.error("Interrupted Exception {}", e.getCause());
            //throw new RuntimeException(e);
        }

        log.info("Success {}, Failed {}", completedCounter.atomicSuccessInteger.get(), completedCounter.atomicFailedInteger.get());

        return ResponseEntity.ok()
                //.contentType(MediaType.APPLICATION_STREAM_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body("done");
    }

    @GetMapping("/dbuser")
    public ResponseEntity<String> getDbUser() throws SQLException {
        try (Statement statement = userDataSource.getConnection().createStatement()){
            ResultSet rs = statement.executeQuery("SELECT session_user, current_user;");
            if (rs.next()) {
                return ResponseEntity.ok()
                        //.contentType(MediaType.APPLICATION_STREAM_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(rs.getString(1) + "_" + rs.getString(2));
            }
            return ResponseEntity.ok()
                    //.contentType(MediaType.APPLICATION_STREAM_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("NotFound");
        }
    }
}
