package com.suchika.finance;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "finance")
public class FinanceApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}