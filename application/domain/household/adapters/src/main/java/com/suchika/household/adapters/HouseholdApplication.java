package com.suchika.household.adapters;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "household")
public class HouseholdApplication {
    private HouseholdApplication() {}

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
