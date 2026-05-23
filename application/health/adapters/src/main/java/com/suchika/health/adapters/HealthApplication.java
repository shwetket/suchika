package com.suchika.health.adapters;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "health")
public class HealthApplication {
    static void main(String... args) {
        Quarkus.run(args);
    }
}