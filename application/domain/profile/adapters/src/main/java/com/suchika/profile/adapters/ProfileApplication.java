package com.suchika.profile.adapters;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "profile")
public class ProfileApplication {
    private ProfileApplication() {}

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
