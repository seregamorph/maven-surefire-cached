package com.github.seregamorph.testcacheserver;

import org.springframework.boot.SpringApplication;

public class TestTestCacheServerApplication {

    public static void main(String[] args) {
        SpringApplication.from(TestCacheServerApplication::main)
            .with(TestcontainersConfiguration.class).run(args);
    }

}
