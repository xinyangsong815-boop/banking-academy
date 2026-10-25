package cn.academy.bank.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(new ImportOption.DoNotIncludeTests())
                    .importPackages("cn.academy.bank");

    @Test
    void domainDoesNotDependOnFrameworkOrOuterLayers() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "..web..",
                        "..application..",
                        "..infrastructure..",
                        "..config..")
                .check(classes);
    }

    @Test
    void infrastructureDoesNotDependOnControllers() {
        noClasses()
                .that()
                .resideInAPackage("..infrastructure..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..web..")
                .check(classes);
    }

    @Test
    void controllersNeverUseConcreteRepositoryAdapters() {
        noClasses()
                .that()
                .resideInAPackage("..web..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "org.springframework.jdbc..")
                .check(classes);
    }
}
