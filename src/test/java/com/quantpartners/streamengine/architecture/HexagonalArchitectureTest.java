package com.quantpartners.streamengine.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.quantpartners.streamengine",
    importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule domainMustNotDependOnInfrastructure =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("Hexagonal Architecture: Domain must be purely decoupled from infrastructure adapters");

    @ArchTest
    public static final ArchRule domainMustNotDependOnQuarkusFramework =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("io.quarkus..")
            .because("Domain layer must be framework-agnostic plain Java without framework dependencies");

    @ArchTest
    public static final ArchRule domainMustNotDependOnCamelFramework =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.apache.camel..")
            .because("Domain layer must remain clean from messaging DSL dependencies");
}
