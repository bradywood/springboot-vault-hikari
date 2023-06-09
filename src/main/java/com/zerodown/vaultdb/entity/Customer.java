package com.zerodown.vaultdb.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Customer {

    @Id
    Integer id;
    String name;
    String address;
}
