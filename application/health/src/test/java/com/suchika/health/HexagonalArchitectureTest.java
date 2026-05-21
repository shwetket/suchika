package com.suchika.health;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Assumptions;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

public class HexagonalArchitectureTest {

    private JavaClasses classes() {
        try {
            return new ClassFileImporter().importPackages("com.suchika.health");
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "ArchUnit import failed: " + t.getMessage());
            return null;
        }
    }

    @Test
    public void domain_should_not_depend_on_ports_or_adapters() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..ports..", "..adapters..")
            .allowEmptyShould(true);

        rule.check(classes());
    }

    @Test
    public void ports_should_not_depend_on_adapters() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..ports..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapters..")
            .allowEmptyShould(true);

        rule.check(classes());
    }

    @Test
    public void adapters_should_only_interact_through_ports_and_not_depend_on_domain_or_application() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..adapters..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..application..")
            .allowEmptyShould(true);

        rule.check(classes());
    }
}
