package com.parosurvivors.serviya.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Fronteras entre los modulos del monolito modular.
 *
 * <p>Regla de oro: un modulo solo puede consumir la API publica de otro modulo
 * ({@code <modulo>.api}) o el contrato compartido ({@code shared.contract}). Nunca debe importar
 * su {@code domain}, sus puertos de persistencia ({@code application.ports.output}) ni su
 * {@code infrastructure}. La raiz de composicion ({@code config}) es la unica excepcion: puede
 * cablear implementaciones de cualquier modulo.</p>
 *
 * <p>Las reglas que hoy tienen violaciones se envuelven en {@link FreezingArchRule}: el baseline
 * actual queda congelado en {@code src/test/resources/archunit_store} y cualquier dependencia
 * NUEVA rompe el build. A medida que se corrige el acoplamiento, el baseline se reduce hasta poder
 * eliminar el freeze. Ver {@code archunit.properties} para actualizar el baseline a proposito.</p>
 *
 * <p>Se usa JUnit 5 directamente (en lugar del TestEngine propio de ArchUnit) para que las reglas
 * se ejecuten con el mismo runner que el resto de la suite y no dependan de la version del
 * JUnit Platform que gestione Spring Boot.</p>
 *
 * <p><strong>Pendiente (fase posterior):</strong> la deteccion de ciclos entre modulos. La regla
 * {@code slices().should().beFreeOfCycles()} de ArchUnit produce violaciones con orden no
 * determinista, por lo que no es apta para {@link FreezingArchRule}; se sustituira por un test
 * determinista de la matriz de dependencias cuando se aborde la rotura de ciclos (paso 5 del plan).</p>
 */
public class ModuleBoundaryTest {

    /** Modulos de negocio. {@code shared} (kernel) y {@code config} (composition root) se tratan aparte. */
    private static final List<String> MODULES = List.of(
            "admin",
            "feedback",
            "metrics",
            "notifications",
            "profiles",
            "reports",
            "requests",
            "services",
            "users");

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.parosurvivors.serviya");

    private static final ArchRule NO_FOREIGN_INFRASTRUCTURE =
            FreezingArchRule.freeze(noForeignLayer(
                    "infrastructure",
                    "Un modulo no debe usar la infraestructura de otro modulo; debe pasar por su API"));

    private static final ArchRule NO_FOREIGN_PERSISTENCE_PORTS =
            FreezingArchRule.freeze(noForeignLayer(
                    "application.ports.output",
                    "Un modulo no debe usar los puertos de persistencia de otro modulo; debe usar una API de consulta"));

    private static final ArchRule NO_FOREIGN_DOMAIN =
            FreezingArchRule.freeze(noForeignLayer(
                    "domain",
                    "Un modulo no debe compartir entidades de dominio; debe usar DTOs de su API"));

    private static final ArchRule SHARED_WITHOUT_BUSINESS_MODULES =
            FreezingArchRule.freeze(noClasses()
                    .that().resideInAPackage("..serviya.shared..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            MODULES.stream()
                                    .map(module -> "..serviya." + module + "..")
                                    .toArray(String[]::new))
                    .because("shared es el kernel transversal y no debe conocer modulos de negocio"));

    @Test
    void modulesMustNotDependOnForeignInfrastructure() {
        NO_FOREIGN_INFRASTRUCTURE.check(CLASSES);
    }

    @Test
    void modulesMustNotDependOnForeignPersistencePorts() {
        NO_FOREIGN_PERSISTENCE_PORTS.check(CLASSES);
    }

    @Test
    void modulesMustNotDependOnForeignDomain() {
        NO_FOREIGN_DOMAIN.check(CLASSES);
    }

    @Test
    void sharedMustNotDependOnBusinessModules() {
        SHARED_WITHOUT_BUSINESS_MODULES.check(CLASSES);
    }

    @Test
    void apiPackagesMustBePure() {
        modulesMustExposePureApis().check(CLASSES);
    }

    @Test
    void domainMustNotDependOnApplicationOrInfrastructure() {
        noClasses()
                .that().resideInAPackage("..serviya..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..serviya..application..",
                        "..serviya..infrastructure..")
                .because("el dominio debe ser independiente de las capas de aplicacion e infraestructura")
                .check(CLASSES);
    }

    @Test
    void applicationMustNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..serviya..application..")
                .should().dependOnClassesThat().resideInAPackage("..serviya..infrastructure..")
                .because("la capa de aplicacion no debe depender de infraestructura (puertos y adaptadores)")
                .check(CLASSES);
    }

    /**
     * Prohibe que un modulo dependa de una capa concreta de otro modulo.
     *
     * <p>Se genera una regla por modulo destino para poder expresar "cualquier origen excepto el
     * propio destino". El resultado se compone en una sola regla para que el freeze la trate como
     * una unidad.</p>
     */
    private static ArchRule noForeignLayer(String layer, String because) {
        ArchRule combined = null;
        for (String target : MODULES) {
            String[] sources = MODULES.stream()
                    .filter(module -> !module.equals(target))
                    .map(module -> "..serviya." + module + "..")
                    .toArray(String[]::new);

            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(sources)
                    .should().dependOnClassesThat()
                    .resideInAPackage("..serviya." + target + "." + layer + "..")
                    .because(because + " (modulo destino: " + target + ")");

            combined = combined == null ? rule : CompositeArchRule.of(combined).and(rule);
        }
        return combined;
    }

    /**
     * Una API publica debe ser un contrato estable y extraible: solo puede depender de JDK,
     * anotaciones de Spring, del propio modulo y de {@code shared.contract}. Nunca de entidades
     * JPA, mappers de infraestructura ni dominio interno de otro modulo.
     */
    private static ArchRule modulesMustExposePureApis() {
        ArchRule combined = null;
        for (String module : MODULES) {
            ArchRule rule = classes()
                    .that().resideInAPackage("..serviya." + module + ".api..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            "javax..",
                            "jakarta..",
                            "org.springframework..",
                            "..serviya." + module + ".api..",
                            "..serviya.shared.contract..")
                    .because("la API de un modulo debe ser un contrato estable y extraible (modulo: "
                            + module + ")")
                    // Todavia no existe ningun paquete <modulo>.api; la regla debe pasar vacia
                    // hasta que se introduzcan los primeros contratos.
                    .allowEmptyShould(true);

            combined = combined == null ? rule : CompositeArchRule.of(combined).and(rule);
        }
        return combined;
    }
}
