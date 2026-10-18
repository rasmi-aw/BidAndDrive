package com.auction.app.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal boundaries across all modules (production classes only).
 */
@AnalyzeClasses(packages = "com.auction", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "com.auction.domain..";
    private static final String APPLICATION = "com.auction.application..";
    private static final String COMMANDS = "com.auction.application.command..";
    private static final String QUERIES = "com.auction.application.query..";
    private static final String INFRASTRUCTURE = "com.auction.infrastructure..";
    private static final String APP = "com.auction.app..";

    @ArchTest
    static final ArchRule domain_does_not_depend_on_spring_or_jpa =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta.persistence..", "org.hibernate..")
                    .because("the domain is pure Java; frameworks live in infrastructure and app");

    // "lombok" (root package only) is allowed for @lombok.Generated, a CLASS-retention marker Lombok adds to generated
    // methods; Lombok is a compile-only (provided) dependency, so the domain still needs nothing but the JDK at runtime.
    @ArchTest
    static final ArchRule domain_depends_only_on_the_jdk_and_itself =
            classes().that().resideInAPackage(DOMAIN)
                    .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..", "lombok")
                    .because("the domain depends on nothing outside itself");

    @ArchTest
    static final ArchRule application_depends_only_on_the_domain_and_the_jdk =
            classes().that().resideInAPackage(APPLICATION)
                    .should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, "java..", "lombok")
                    .because("use cases are framework free; the app module wires them");

    @ArchTest
    static final ArchRule queries_do_not_depend_on_commands =
            noClasses().that().resideInAPackage(QUERIES)
                    .should().dependOnClassesThat().resideInAPackage(COMMANDS)
                    .because("CQRS: command and query use cases are kept separate");

    @ArchTest
    static final ArchRule commands_do_not_depend_on_queries =
            noClasses().that().resideInAPackage(COMMANDS)
                    .should().dependOnClassesThat().resideInAPackage(QUERIES)
                    .because("CQRS: command and query use cases are kept separate");

    @ArchTest
    static final ArchRule infrastructure_does_not_depend_on_app =
            noClasses().that().resideInAPackage(INFRASTRUCTURE)
                    .should().dependOnClassesThat().resideInAPackage(APP)
                    .because("adapters must not know about controllers or app configuration")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_do_not_use_repositories =
            noClasses().that().areAnnotatedWith(RestController.class).or().areAnnotatedWith(Controller.class)
                    .should().dependOnClassesThat(describe("are repositories",
                            assignableTo(Repository.class)
                                    .or(annotatedWith(org.springframework.stereotype.Repository.class))
                                    .or(simpleNameEndingWith("Repository"))))
                    .because("controllers go through command/query use cases, never repositories or repository ports")
                    .allowEmptyShould(true);
}
