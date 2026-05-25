package com.suchika.finance.adapters;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "finance")
public class FinanceApplication {
    static void main(String... args) {
        Quarkus.run(args);
    }
}