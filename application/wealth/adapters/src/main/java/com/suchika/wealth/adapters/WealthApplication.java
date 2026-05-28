package com.suchika.wealth.adapters;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "wealth")
public class WealthApplication {
    static void main(String... args) {
        Quarkus.run(args);
    }
}