package com.zerodown.vaultdb.stats;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@NoArgsConstructor
public class CompletedCounter {

    public AtomicInteger atomicSuccessInteger = new AtomicInteger(0);

    public AtomicInteger atomicFailedInteger = new AtomicInteger(0);

}
