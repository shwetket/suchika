package com.suchika.health;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "health")
public class HealthApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}