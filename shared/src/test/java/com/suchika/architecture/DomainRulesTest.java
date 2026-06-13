package com.suchika.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainRulesTest {

    @Test
    void domainPackagesShouldNotDependOnFrameworks() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.suchika");

        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "javax.persistence..", "jakarta.persistence..",
                "org.hibernate..",
                "io.quarkus.."
            )
            .allowEmptyShould(true)
            .check(classes);
    }
}
