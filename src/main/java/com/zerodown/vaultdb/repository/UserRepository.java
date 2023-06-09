package com.zerodown.vaultdb.repository;

import com.zerodown.vaultdb.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Customer, Integer> {

}
