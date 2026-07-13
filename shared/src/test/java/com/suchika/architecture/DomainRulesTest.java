package com.suchika.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural fitness functions for the Suchika project.
 *
 * <p>These tests enforce the hexagonal architecture (Ports & Adapters) pattern.
 * Every rule is expressed as a failing test — violations are caught at build time,
 * not during code review. All rules apply equally whether the author is a human
 * developer or an AI code-generation tool.
 *
 * <hr>
 * <h2>Package Structure — read this before writing any new class</h2>
 *
 * <pre>
 * com.suchika.{domain}.domain.*    — Pure Java business logic. ZERO framework deps.
 *                                    No @Inject. No JPA. No HTTP. No Quarkus.
 *                                    Only Java standard library + shared module.
 *
 * com.suchika.{domain}.ports.*     — Interface contracts (Java interfaces only).
 *   .ports.input.*                 — Use-case interfaces called by adapters inward.
 *   .ports.output.*                — Persistence/external interfaces implemented by adapters.
 *
 * com.suchika.{domain}.adapters.*  — Framework-aware outer layer.
 *                                    HTTP controllers (Quarkus REST / JAX-RS).
 *                                    JPA/Panache entity classes + repositories.
 *                                    Flyway migrations run from here.
 *
 * com.suchika.shared.*             — Cross-cutting utilities used by all layers.
 *                                    AppLogger, typed exception hierarchy.
 *                                    Must NOT import from any domain module.
 * </pre>
 *
 * Where {domain} is one of: profile | wealth | household | health
 *
 * <hr>
 * <h2>Dependency Direction (STRICT — never reverse these arrows)</h2>
 *
 * <pre>
 *   adapters  →  ports  →  domain
 *       ↓                    ↑
 *      shared ────────────────┘
 *
 *   adapters implement output ports (persistence adapters).
 *   adapters call input ports (use-case interfaces).
 *   domain and ports have NO knowledge of adapters or frameworks.
 *   shared is a leaf module with NO domain-specific imports.
 * </pre>
 *
 * <hr>
 * <h2>Cross-Domain Rule (NO cross-domain imports)</h2>
 *
 * <pre>
 *   wealth  ──────────────────────────────╮
 *   household ─────────────────────────── ├─ MAY import shared.*
 *   health  ──────────────────────────────╯
 *   profile ──────────────────────────────╯
 *
 *   wealth.domain.*  must NOT import health.*, household.*, profile.*
 *   health.domain.*  must NOT import wealth.*, household.*, profile.*
 *   (same for all domain combinations)
 *
 *   Cross-domain data aggregation is the job of web-gateway (the BFF).
 *   It reads from domain REST APIs or the projections schema — never via
 *   direct Java class imports across module boundaries.
 * </pre>
 */
class DomainRulesTest {

    private static JavaClasses allClasses;
    private static JavaClasses nonTestClasses;

    @BeforeAll
    static void importClasses() {
        // Imports all com.suchika.* classes visible on the test runtime classpath.
        // In a multi-module Gradle build, each domain's adapters module should
        // declare testImplementation(project(":shared")) to run these rules
        // against its own compiled classes.
        allClasses = new ClassFileImporter().importPackages("com.suchika");

        // Same scan, minus test classes (Gradle's build/classes/java/test/... output).
        // Used by rules that must not flag test-only tooling — e.g. a test class
        // attaching a java.util.logging.Handler to assert on log level is
        // verification tooling, not a violation of "use AppLogger in application code".
        nonTestClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.suchika");
    }


    // =========================================================================
    // GROUP 1: DOMAIN LAYER PURITY
    // The domain layer is the innermost ring. It must be framework-agnostic so
    // it can be tested without starting Quarkus, and ported to another framework
    // without changing business logic.
    // =========================================================================

    /**
     * The domain layer must not depend on any persistence framework.
     *
     * <p>Rationale: JPA annotations (@Entity, @Column) in domain classes create
     * a tight coupling between business logic and the ORM. Domain objects must
     * be plain Java classes that can be instantiated in unit tests without a
     * database or Hibernate session.
     *
     * <p>Correct: {@code domain/} contains plain Java classes.
     * Wrong:     {@code domain/} class annotated with {@code @Entity} or
     *            {@code @Column}, or importing {@code jakarta.persistence.*}.
     */
    @Test
    void domain_must_not_depend_on_persistence_frameworks() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta.persistence..",
                "javax.persistence..",
                "org.hibernate..",
                "io.quarkus.hibernate..",
                "io.quarkus.panache.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * The domain layer must not depend on HTTP or CDI framework classes.
     *
     * <p>Rationale: HTTP types (@Path, @GET, Response) in domain classes would
     * mean business logic knows about its transport protocol — a layering violation.
     * CDI annotations (@Inject, @ApplicationScoped) couple the domain to the
     * DI container, preventing plain-Java unit testing.
     *
     * <p>Correct: domain services are instantiated with {@code new} in tests.
     * Wrong:     domain class annotated with {@code @ApplicationScoped} or
     *            accepting {@code jakarta.ws.rs.core.Response} as a return type.
     */
    @Test
    void domain_must_not_depend_on_http_or_cdi_frameworks() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta.ws.rs..",
                "jakarta.inject..",
                "io.quarkus.arc..",
                "io.quarkus.rest..",
                "io.smallrye.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * The domain layer must not import classes from the ports or adapters layers.
     *
     * <p>Rationale: domain objects represent business concepts. They must not
     * know that they are persisted, served over HTTP, or called via a use-case
     * interface. The dependency arrow points INTO the domain, never out.
     *
     * <p>Correct: domain service takes a plain Java value object as parameter.
     * Wrong:     domain service calls a port interface directly (inversion broken),
     *            or imports an adapter class.
     */
    @Test
    void domain_must_not_depend_on_ports_or_adapters() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..ports..", "..adapters..")
            .allowEmptyShould(true)
            .check(allClasses);
    }


    // =========================================================================
    // GROUP 2: PORTS LAYER RULES
    // Ports are the boundary contracts. Input ports = what the domain exposes.
    // Output ports = what the domain needs from the outside world.
    // =========================================================================

    /**
     * The ports layer must not depend on the adapters layer.
     *
     * <p>Rationale: ports define the contract; adapters implement it. A port
     * importing an adapter would create a circular dependency and defeat the
     * purpose of the interface boundary.
     *
     * <p>Correct: port interfaces reference only domain classes and Java types.
     * Wrong:     port interface method returns an adapter-specific JPA entity.
     */
    @Test
    void ports_must_not_depend_on_adapters() {
        noClasses()
            .that().resideInAPackage("..ports..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..")
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * Input port classes must be Java interfaces.
     *
     * <p>Rationale: input ports define use-case contracts. Concrete classes would
     * couple the adapter (caller) to an implementation detail. The adapter must
     * only know the interface — the domain provides the implementation via DI.
     *
     * <p>Convention: place input port interfaces in {@code ..ports.input..} package.
     * Example: {@code com.suchika.wealth.ports.input.CreateAccountUseCase}.
     */
    @Test
    void input_ports_must_be_interfaces() {
        classes()
            .that().resideInAPackage("..ports.input..")
            .should().beInterfaces()
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * Output port classes must be Java interfaces.
     *
     * <p>Rationale: output ports define what the domain needs from persistence or
     * external services. Concrete output port classes would expose the adapter's
     * implementation to the domain layer.
     *
     * <p>Convention: place output port interfaces in {@code ..ports.output..} package.
     * Example: {@code com.suchika.wealth.ports.output.AccountRepository}.
     */
    @Test
    void output_ports_must_be_interfaces() {
        classes()
            .that().resideInAPackage("..ports.output..")
            .should().beInterfaces()
            .allowEmptyShould(true)
            .check(allClasses);
    }


    // =========================================================================
    // GROUP 3: ADAPTERS LAYER RULES
    // Adapters implement ports and translate between domain objects and
    // framework-specific types (JPA entities, HTTP request/response bodies).
    // =========================================================================

    /**
     * JPA @Entity classes must only be in the adapters layer.
     *
     * <p>Rationale: @Entity classes are a persistence implementation detail.
     * They carry ORM annotations, lazy-loading proxies, and lifecycle callbacks
     * that have no place in business logic. The adapters layer maps between
     * JPA entities and domain objects — domain objects themselves are plain Java.
     *
     * <p>Correct: {@code WealthAccountEntity} in {@code adapters.persistence}.
     * Wrong:     {@code Account} (domain object) annotated with {@code @Entity}.
     */
    @Test
    void jpa_entities_must_only_reside_in_adapters() {
        classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..adapters..")
            .allowEmptyShould(true)
            .check(allClasses);
    }


    // =========================================================================
    // GROUP 4: CROSS-DOMAIN ISOLATION
    // Each domain is a bounded context. Business logic in one domain must not
    // directly import classes from another domain's packages.
    //
    // Cross-domain read access is handled by:
    //   - web-gateway (BFF) via REST API calls
    //   - projections.dashboard_snapshot (CQRS read model, JSONB payloads)
    //
    // The profile domain is the identity anchor: other domains hold a profile_id
    // UUID FK, but they do NOT import profile Java classes.
    // =========================================================================

    /**
     * The wealth domain must not import from any other application domain.
     *
     * <p>Permitted imports: {@code com.suchika.shared.*}, Java standard library,
     * framework classes in the adapters layer.
     * Forbidden: direct class imports from health, household, or profile packages.
     */
    @Test
    void wealth_domain_must_not_import_other_domains() {
        noClasses()
            .that().resideInAPackage("com.suchika.wealth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.suchika.health..",
                "com.suchika.household..",
                "com.suchika.profile.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * The health domain must not import from any other application domain.
     */
    @Test
    void health_domain_must_not_import_other_domains() {
        noClasses()
            .that().resideInAPackage("com.suchika.health..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.suchika.wealth..",
                "com.suchika.household..",
                "com.suchika.profile.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * The household domain must not import from any other application domain.
     */
    @Test
    void household_domain_must_not_import_other_domains() {
        noClasses()
            .that().resideInAPackage("com.suchika.household..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.suchika.wealth..",
                "com.suchika.health..",
                "com.suchika.profile.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * The profile domain must not import from any other application domain.
     *
     * <p>Profile is the upstream identity anchor. It is referenced BY other domains
     * via a UUID foreign key ({@code profile_id}). Profile itself must never
     * reference wealth, health, or household concepts — that would create a
     * circular dependency with the domain that depends on it.
     */
    @Test
    void profile_domain_must_not_import_other_domains() {
        noClasses()
            .that().resideInAPackage("com.suchika.profile..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.suchika.wealth..",
                "com.suchika.health..",
                "com.suchika.household.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }


    // =========================================================================
    // GROUP 5: SHARED MODULE RULES
    // The shared module is a leaf: it provides utilities to all other modules
    // but must not depend on any of them.
    // =========================================================================

    /**
     * The shared module must not import any domain-specific module.
     *
     * <p>Rationale: shared is a leaf dependency used by ALL domains. If shared
     * imported from a domain, every other domain would transitively depend on
     * that domain — creating a hidden coupling and preventing independent
     * compilation and deployment of individual domain modules.
     *
     * <p>Permitted in shared: Java standard library, logging facades (internal),
     * Jakarta validation API (for the exception hierarchy).
     * Forbidden: any {@code com.suchika.wealth.*},
     *            {@code com.suchika.health.*}, etc.
     */
    @Test
    void shared_must_not_depend_on_domain_modules() {
        noClasses()
            .that().resideInAPackage("com.suchika.shared..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.suchika.profile..",
                "com.suchika.wealth..",
                "com.suchika.household..",
                "com.suchika.health.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * The shared-adapter module (ADR-023 revision, 2026-07-13) must not
     * import any domain-specific module either. Mirrors {@link
     * #shared_must_not_depend_on_domain_modules()} exactly, scoped to
     * {@code com.suchika.sharedadapter..} instead of {@code com.suchika.shared..}.
     *
     * <p>Rationale: {@code shared-adapter} holds the abstract base classes
     * (JAX-RS resource logic, Panache repository/service query logic) that
     * every domain's own {@code adapters} module subclasses for its {@code
     * error_log} vertical slice. It must stay just as domain-agnostic as
     * {@code shared} itself -- parameterized only by generics/abstract
     * hooks the concrete per-domain subclass supplies, never by importing a
     * domain's own classes directly.
     */
    @Test
    void shared_adapter_must_not_depend_on_domain_modules() {
        noClasses()
            .that().resideInAPackage("com.suchika.sharedadapter..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.suchika.profile..",
                "com.suchika.wealth..",
                "com.suchika.household..",
                "com.suchika.health.."
            )
            .allowEmptyShould(true)
            .check(allClasses);
    }


    // =========================================================================
    // GROUP 6: LOGGING RULES
    // All application logging must go through AppLogger (shared module).
    // Direct use of SLF4J or java.util.logging creates inconsistent log
    // formatting and bypasses the structured logging pattern.
    // =========================================================================

    /**
     * Application code must not use raw SLF4J, JUL, or JBoss Logging loggers.
     * Use {@code com.suchika.shared.logging.AppLogger} instead.
     *
     * <p>Rationale: AppLogger enforces a consistent structured log format
     * across all domains, and locks in the project's "exactly 4 conventions
     * — INFO/WARNING/ERROR/HEALTH, no DEBUG" logging rule. Direct logger usage
     * creates ad-hoc log messages that bypass that convention and are harder
     * to parse and correlate in log aggregation tools.
     *
     * <p>Correct: {@code AppLogger.info("Account created: %s", accountId);}
     * Wrong:     {@code private static final Logger log = LoggerFactory.getLogger(...);}
     *
     * <p>Only {@code AppLogger.java} itself is exempted — it IS the logger
     * wrapper (it legitimately depends on {@code io.quarkus.logging.Log} and,
     * for the HEALTH category, {@code org.jboss.logging.Logger}). Every other
     * production class in {@code com.suchika.shared} (and every domain) must
     * go through it. Checked against {@link #nonTestClasses} rather than
     * {@link #allClasses} — test classes are legitimately allowed to attach a
     * raw {@code java.util.logging.Handler} as verification tooling (e.g.
     * {@code ApplicationExceptionMapperTest} asserts on log level that way);
     * that isn't application logging and would otherwise false-positive
     * (including via ArchUnit-synthesized anonymous inner classes, whose
     * simple name doesn't end in "Test" and so can't be filtered by a naming
     * convention — hence the source-set-based exclusion instead).
     */
    @Test
    void application_code_must_not_use_raw_loggers() {
        noClasses()
            .that().resideInAPackage("com.suchika..")
            .and().doNotHaveFullyQualifiedName("com.suchika.shared.logging.AppLogger")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.slf4j..",
                "java.util.logging..",
                "org.jboss.logging.."
            )
            .allowEmptyShould(true)
            .check(nonTestClasses);
    }

    /**
     * {@code AppLogger} must never declare a method named {@code debug} —
     * DEBUG level logging is banned project-wide (2026-07 logging convention:
     * exactly INFO / WARNING / ERROR / HEALTH, nothing else). Confirmed by grep
     * that zero call sites used it before this rule was added; this locks the
     * clean state in at compile time.
     */
    @Test
    void app_logger_must_not_declare_a_debug_method() {
        classes()
            .that().haveFullyQualifiedName("com.suchika.shared.logging.AppLogger")
            .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                "not declare a method named 'debug'") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass clazz, com.tngtech.archunit.lang.ConditionEvents events) {
                    clazz.getMethods().stream()
                        .filter(method -> "debug".equals(method.getName()))
                        .forEach(method -> events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(method,
                            "AppLogger must not declare a debug() method — DEBUG level logging is banned project-wide")));
                }
            })
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * No application class may call a method named {@code debug(...)}, on any
     * type. Belt-and-suspenders companion to
     * {@link #app_logger_must_not_declare_a_debug_method()}: even if some other
     * class introduced its own {@code debug(...)} method (logging or otherwise
     * named to slip past that rule), calling it from application code is
     * still a signal that DEBUG-level logging is creeping back in and is
     * flagged here.
     */
    @Test
    void application_code_must_not_call_debug_methods() {
        classes()
            .that().resideInAPackage("com.suchika..")
            .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                "not call any method named 'debug'") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass clazz, com.tngtech.archunit.lang.ConditionEvents events) {
                    clazz.getMethodCallsFromSelf().stream()
                        .filter(call -> "debug".equals(call.getTarget().getName()))
                        .forEach(call -> events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(call,
                            clazz.getName() + " calls a method named debug() — DEBUG level logging is banned project-wide")));
                }
            })
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * Web gateway resource classes must have corresponding test classes.
     *
     * <p>Rationale: Enforces that developer agents cannot write new gateway
     * resources without also writing API/integration tests.
     */
    @Test
    void gateway_resources_must_have_corresponding_test() {
        classes()
            .that().resideInAPackage("com.suchika.gateway..")
            .and().haveSimpleNameEndingWith("Resource")
            .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>("have a corresponding test class") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass resourceClass, com.tngtech.archunit.lang.ConditionEvents events) {
                    String testClassName = resourceClass.getName() + "Test";
                    String itClassName = resourceClass.getName() + "IT";
                    boolean exists = false;
                    for (com.tngtech.archunit.core.domain.JavaClass clazz : allClasses) {
                        if (clazz.getName().equals(testClassName) || clazz.getName().equals(itClassName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(resourceClass,
                            "Gateway resource " + resourceClass.getName() + " does not have a corresponding test class (e.g. " + testClassName + ")"));
                    }
                }
            })
            .allowEmptyShould(true)
            .check(allClasses);
    }

    /**
     * Every use-case interface in ports.input must be referenced by at least one
     * test class — either a stub implementation in a resource test, or a direct
     * reference in a service test.
     *
     * <p>Rationale: v0.6 testing-foundation gap (re-scoped from the original
     * "unit tests for all domain use cases" milestone item). Enforces that every
     * use-case contract has some test coverage rather than being silently
     * unexercised. A "reference" is any ArchUnit-tracked dependency (implements,
     * field type, method parameter, etc.) from a class whose simple name ends in
     * Test or IT — service tests that only reference the concrete *Service class
     * (never the interface type itself) do not count, by design: that gap is
     * exactly what this rule exists to catch.
     */
    @Test
    void ports_input_interfaces_must_be_referenced_by_a_test_class() {
        classes()
            .that().resideInAPackage("..ports.input..")
            .and().areInterfaces()
            .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>("be referenced by at least one test class") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass portInterface, com.tngtech.archunit.lang.ConditionEvents events) {
                    boolean referencedByTest = portInterface.getDirectDependenciesToSelf().stream()
                        .map(com.tngtech.archunit.core.domain.Dependency::getOriginClass)
                        .anyMatch(origin -> origin.getSimpleName().endsWith("Test") || origin.getSimpleName().endsWith("IT"));
                    if (!referencedByTest) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(portInterface,
                            "Use-case interface " + portInterface.getName() + " is not referenced by any test class"));
                    }
                }
            })
            .allowEmptyShould(true)
            .check(allClasses);
    }
}

