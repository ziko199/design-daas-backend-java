package de.frauas.design.backend.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

/**
 * ArchUnit tests enforcing architecture constraints of the design-daas-backend.
 */
@AnalyzeClasses(
        packages = "de.frauas.design.backend",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    // -------------------------------------------------------------------------
    // Layered Architecture
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule layerDependencies = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Config")      .definedBy("de.frauas.design.backend.config..")
            .layer("Auth")        .definedBy("de.frauas.design.backend.auth..")
            .layer("Controllers") .definedBy("de.frauas.design.backend..controller..")
            .layer("Services")    .definedBy("de.frauas.design.backend..service..")
            .layer("Repositories").definedBy("de.frauas.design.backend..repository..")
            .layer("Models")      .definedBy("de.frauas.design.backend..model..")
            .layer("DTOs")        .definedBy("de.frauas.design.backend..dto..")
            .layer("Shared")      .definedBy("de.frauas.design.backend.shared..")

            .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
            .whereLayer("Services")   .mayOnlyBeAccessedByLayers("Controllers", "Config", "Auth", "Services")
            .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services", "Config", "Auth", "Controllers")
            .whereLayer("Models")     .mayOnlyBeAccessedByLayers("Controllers", "Services", "Repositories", "Config", "Auth", "DTOs");

    // -------------------------------------------------------------------------
    // Naming Conventions
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule servicesShouldBeSuffixedService =
            classes().that().areAnnotatedWith(Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("ServiceImpl")
                    .because("Spring @Service classes should be suffixed with Service or ServiceImpl");

    @ArchTest
    static final ArchRule repositoriesShouldBeSuffixedRepository =
            classes().that().areAnnotatedWith(Repository.class)
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Spring @Repository classes should be suffixed with Repository");

    @ArchTest
    static final ArchRule controllersShouldBeSuffixedController =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Spring @RestController classes should be suffixed with Controller");

    // -------------------------------------------------------------------------
    // Package Rules
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule servicesShouldResideInServicePackage =
            classes().that().areAnnotatedWith(Service.class)
                    .should().resideInAPackage("..service..")
                    // Auth-scoped services (e.g. TokenCleanupService) live in the auth package by design
                    .orShould().resideInAPackage("de.frauas.design.backend.auth..")
                    .because("@Service beans must live in a service or auth package");

    @ArchTest
    static final ArchRule repositoriesShouldResideInRepositoryPackage =
            classes().that().areAnnotatedWith(Repository.class)
                    .should().resideInAPackage("..repository..")
                    // RefreshTokenRepository lives in auth package by design
                    .orShould().resideInAPackage("de.frauas.design.backend.auth..")
                    .because("@Repository beans must live in a repository or auth package");

    @ArchTest
    static final ArchRule controllersShouldResideInControllerPackage =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..controller..")
                    .orShould().resideInAPackage("de.frauas.design.backend.auth..")
                    .because("@RestController beans must live in a controller or auth package");

    // -------------------------------------------------------------------------
    // General Coding Rules
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule noJavaUtilLogging =
            NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
                    .because("Use SLF4J (@Slf4j) for logging");

    @ArchTest
    static final ArchRule noSystemOut =
            NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                    .because("Use SLF4J (@Slf4j) for output, not System.out/err");

    @ArchTest
    static final ArchRule noJodaTime =
            NO_CLASSES_SHOULD_USE_JODATIME
                    .because("Use java.time (JSR-310) instead of Joda-Time");

    // -------------------------------------------------------------------------
    // Dependency Rules
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule modelsMustNotDependOnServices =
            noClasses().that().resideInAPackage("..model..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .because("Domain models must not depend on the service layer");

    @ArchTest
    static final ArchRule modelsMustNotDependOnControllers =
            noClasses().that().resideInAPackage("..model..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Domain models must not depend on controllers");

    @ArchTest
    static final ArchRule repositoriesMustNotDependOnServices =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .because("Repositories must not depend on services");

    @ArchTest
    static final ArchRule dtosMustNotDependOnRepositories =
            noClasses().that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("DTOs must not depend on repositories");

    // -------------------------------------------------------------------------
    // Transactional Annotation Rules
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule transactionalOnlyOnServicesOrRepositories =
            noClasses().that()
                    .resideInAPackage("..controller..")
                    .should().beAnnotatedWith(Transactional.class)
                    .because("@Transactional should be placed on service or repository layer, not controllers");

    // -------------------------------------------------------------------------
    // Field injection not allowed (prefer constructor injection)
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule noFieldInjectionInServices =
            noFields().that().areDeclaredInClassesThat().resideInAPackage("..service..")
                    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                    .because("Use constructor injection (e.g. @RequiredArgsConstructor) instead of @Autowired field injection in services");
}

