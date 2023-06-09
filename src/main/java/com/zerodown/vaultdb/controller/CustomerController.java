package com.zerodown.vaultdb.controller;

import com.zerodown.vaultdb.entity.Customer;
import com.zerodown.vaultdb.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("/api")
public class CustomerController {

    @Autowired
    private DataSource userDataSource;

    @Autowired
    private UserRepository userRepository;

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
