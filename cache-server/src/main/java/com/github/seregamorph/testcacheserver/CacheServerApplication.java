package com.github.seregamorph.testcacheserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Sergey Chernov
 */
@SpringBootApplication
public class CacheServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheServerApplication.class, args);
    }
}
