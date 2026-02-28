package com.github.seregamorph.testcacheserver;

import org.springframework.boot.SpringApplication;

public class TestCacheServerApplication {

    public static void main(String[] args) {
        SpringApplication.from(CacheServerApplication::main)
            .with(TestcontainersConfiguration.class).run(args);
    }
}
