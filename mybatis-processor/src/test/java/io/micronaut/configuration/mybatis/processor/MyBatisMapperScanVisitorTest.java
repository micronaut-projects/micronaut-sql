/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.mybatis.processor;

import io.micronaut.annotation.processing.TypeElementVisitorProcessor;
import io.micronaut.configuration.mybatis.MyBatisMapperScanRegistration;
import io.micronaut.core.graal.GraalReflectionConfigurer;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.graal.reflect.GraalTypeElementVisitor;
import io.micronaut.inject.visitor.TypeElementVisitor;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisMapperScanVisitorTest {

    @Test
    void generatesRegistrationForDiscoveredMappers(@TempDir Path temporaryDirectory) throws Exception {
        Compilation compilation = compile(temporaryDirectory, List.of(
            new InMemoryJavaFileObject("example.config.MapperConfiguration", """
                package example.config;

                import io.micronaut.configuration.mybatis.MyBatisMapperScan;

                @MyBatisMapperScan(value = "example.mappers", datasource = "orders", mappers = example.other.OtherMapper.class)
                class MapperConfiguration {
                }
                """),
            new InMemoryJavaFileObject("example.domain.Genre", """
                package example.domain;

                public class Genre {
                    private Long id;
                    private String name;
                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
                """),
            new InMemoryJavaFileObject("example.domain.Book", """
                package example.domain;

                public record Book(Long id, String title) {
                }
                """),
            new InMemoryJavaFileObject("example.mappers.GenreMapper", """
                package example.mappers;

                import example.domain.Book;
                import example.domain.Genre;
                import org.apache.ibatis.annotations.Select;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;

                public interface GenreMapper {
                    @Select("select 1")
                    int findOne();
                    @Select("select * from genre")
                    List<Genre> findAll();
                    @Select("select * from book")
                    Optional<Map<String, Book>> findBooks();
                    void save(Genre genre, String[] tags, long id);
                }
                """),
            new InMemoryJavaFileObject("example.mappers.nested.NestedMapper", """
                package example.mappers.nested;

                public interface NestedMapper {
                }
                """),
            new InMemoryJavaFileObject("example.mappers.PackagePrivateMapper", """
                package example.mappers;

                interface PackagePrivateMapper {
                }
                """),
            new InMemoryJavaFileObject("example.mappers.Mappers", """
                package example.mappers;

                public final class Mappers {
                    public interface InnerMapper {
                    }
                    private interface PrivateMapper {
                    }
                    public static class Helper {
                        public interface DeepMapper {
                        }
                    }
                }
                """),
            new InMemoryJavaFileObject("example.other.OtherMapper", """
                package example.other;

                public interface OtherMapper {
                }
                """),
            new InMemoryJavaFileObject("example.other.NotScannedMapper", """
                package example.other;

                public interface NotScannedMapper {
                }
                """)
        ));

        assertTrue(compilation.success(), compilation.diagnostics());
        // one registration per mapper package, so that non-public mappers can be referenced, plus the
        // explicitly listed mappers in the package of the annotated type
        assertTrue(Files.exists(compilation.classes().resolve(
            "example/config/MapperConfiguration$MyBatisMapperScanRegistration.class")));
        assertTrue(Files.exists(compilation.classes().resolve(
            "example/mappers/example_pconfig_pMapperConfiguration$MyBatisMapperScanRegistration.class")));
        assertTrue(Files.exists(compilation.classes().resolve(
            "example/mappers/nested/example_pconfig_pMapperConfiguration$MyBatisMapperScanRegistration.class")));

        try (URLClassLoader classLoader = compilation.classLoader()) {
            List<MyBatisMapperScanRegistration> registrations = new ArrayList<>();
            SoftServiceLoader.load(MyBatisMapperScanRegistration.class, classLoader).collectAll(registrations);

            assertEquals(3, registrations.size());
            Configuration configuration = new Configuration();
            for (MyBatisMapperScanRegistration registration : registrations) {
                assertEquals("orders", registration.getDatasourceName());
                registration.register(configuration);
            }
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.GenreMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.PackagePrivateMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.nested.NestedMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers$InnerMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers$PrivateMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers$Helper$DeepMapper")));
            assertFalse(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.other.OtherMapper")));
            assertFalse(configuration.hasMapper(classLoader.loadClass("example.other.NotScannedMapper")));

            // registering twice must not fail with a MyBatis "already known" error
            for (MyBatisMapperScanRegistration registration : registrations) {
                registration.register(configuration);
            }
        }

        try (URLClassLoader classLoader = compilation.classLoader()) {
            RecordingReflectionContext reflection = reflectionConfiguration(classLoader, "example.config.$MapperConfiguration");
            assertEquals(Set.of(
                "example.mappers.GenreMapper",
                "example.mappers.Mappers$Helper$DeepMapper",
                "example.mappers.Mappers$InnerMapper",
                "example.mappers.Mappers$PrivateMapper",
                "example.mappers.PackagePrivateMapper",
                "example.mappers.nested.NestedMapper",
                "example.other.OtherMapper"
            ), reflection.proxies);
            // GraalReflectionConfigurer registers every configured type, the mappers included
            assertTrue(reflection.types.containsAll(Set.of("example.domain.Book", "example.domain.Genre")), reflection.types.toString());
            assertFalse(reflection.types.contains("example.other.NotScannedMapper"), reflection.types.toString());
            assertFalse(reflection.types.contains("java.lang.String"), reflection.types.toString());
            assertTrue(reflection.members.contains("example.domain.Genre#setName"), reflection.members.toString());
            assertTrue(reflection.members.contains("example.domain.Genre#<init>"), reflection.members.toString());
            assertTrue(reflection.members.contains("example.domain.Genre.name"), reflection.members.toString());
        }
    }

    @Test
    void registersExplicitMappersWithoutPackages(@TempDir Path temporaryDirectory) throws Exception {
        Compilation compilation = compile(temporaryDirectory, List.of(
            new InMemoryJavaFileObject("example.config.MapperConfiguration", """
                package example.config;

                import io.micronaut.configuration.mybatis.MyBatisMapperScan;

                @MyBatisMapperScan(mappers = {example.other.OtherMapper.class, example.other.SecondMapper.class}, nativeImageMetadata = false)
                class MapperConfiguration {
                }
                """),
            new InMemoryJavaFileObject("example.other.OtherMapper", """
                package example.other;

                public interface OtherMapper {
                }
                """),
            new InMemoryJavaFileObject("example.other.SecondMapper", """
                package example.other;

                public interface SecondMapper {
                }
                """),
            new InMemoryJavaFileObject("example.other.NotListedMapper", """
                package example.other;

                public interface NotListedMapper {
                }
                """)
        ));

        assertTrue(compilation.success(), compilation.diagnostics());
        assertFalse(compilation.diagnostics().contains("No mapper interface found"));

        try (URLClassLoader classLoader = compilation.classLoader()) {
            List<MyBatisMapperScanRegistration> registrations = new ArrayList<>();
            SoftServiceLoader.load(MyBatisMapperScanRegistration.class, classLoader).collectAll(registrations);

            assertEquals(1, registrations.size());
            assertEquals("default", registrations.get(0).getDatasourceName());

            Configuration configuration = new Configuration();
            registrations.get(0).register(configuration);
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.other.OtherMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.other.SecondMapper")));
            assertFalse(configuration.hasMapper(classLoader.loadClass("example.other.NotListedMapper")));
        }

        // nativeImageMetadata = false: no reflection configuration is generated
        assertFalse(Files.exists(compilation.classes().resolve("example/config/$MapperConfiguration$ReflectConfig.class")));
    }

    @Test
    void registrationNamesOfSimilarTypesDoNotCollide(@TempDir Path temporaryDirectory) throws Exception {
        Compilation compilation = compile(temporaryDirectory, List.of(
            new InMemoryJavaFileObject("a.b_.c.Config", """
                package a.b_.c;

                @io.micronaut.configuration.mybatis.MyBatisMapperScan("example.mappers")
                class Config {
                }
                """),
            new InMemoryJavaFileObject("a.b._c.Config", """
                package a.b._c;

                @io.micronaut.configuration.mybatis.MyBatisMapperScan(value = "example.mappers", datasource = "second")
                class Config {
                }
                """),
            new InMemoryJavaFileObject("example.mappers.Outer$Inner", """
                package example.mappers;

                // a top-level type whose name contains a dollar, as a nested type's binary name does
                @io.micronaut.configuration.mybatis.MyBatisMapperScan(value = "example.mappers", datasource = "third")
                class Outer$Inner {
                }
                """),
            new InMemoryJavaFileObject("example.mappers.Outer_Inner", """
                package example.mappers;

                @io.micronaut.configuration.mybatis.MyBatisMapperScan(value = "example.mappers", datasource = "fourth")
                class Outer_Inner {
                }
                """),
            new InMemoryJavaFileObject("example.mappers.GenreMapper", """
                package example.mappers;

                public interface GenreMapper {
                }
                """)
        ));

        assertTrue(compilation.success(), compilation.diagnostics());
        for (String registration : List.of(
            "a_pb_u_pc_pConfig",
            "a_pb_p_uc_pConfig",
            "Outer_dInner",
            "Outer_uInner")) {
            assertTrue(Files.exists(compilation.classes().resolve(
                "example/mappers/" + registration + "$MyBatisMapperScanRegistration.class")), registration);
        }

        try (URLClassLoader classLoader = compilation.classLoader()) {
            List<MyBatisMapperScanRegistration> registrations = new ArrayList<>();
            SoftServiceLoader.load(MyBatisMapperScanRegistration.class, classLoader).collectAll(registrations);
            assertEquals(Set.of("default", "second", "third", "fourth"),
                registrations.stream().map(MyBatisMapperScanRegistration::getDatasourceName).collect(Collectors.toSet()));
        }
    }

    @Test
    void warnsAboutPackagesWithoutMappers(@TempDir Path temporaryDirectory) throws Exception {
        Compilation compilation = compile(temporaryDirectory, List.of(
            new InMemoryJavaFileObject("example.config.MapperConfiguration", """
                package example.config;

                import io.micronaut.configuration.mybatis.MyBatisMapperScan;

                @MyBatisMapperScan("example.missing")
                class MapperConfiguration {
                }
                """)
        ));

        assertTrue(compilation.success(), compilation.diagnostics());
        assertTrue(compilation.diagnostics().contains("No mapper interface found in package [example.missing]"));
        assertTrue(Files.exists(compilation.classes().resolve(
            "example/config/MapperConfiguration$MyBatisMapperScanRegistration.class")));
    }

    @Test
    void visitsAllClassesAndAggregates() {
        MyBatisMapperScanVisitor visitor = new MyBatisMapperScanVisitor();

        assertEquals(Set.of("*"), visitor.getSupportedAnnotationNames());
        assertEquals(TypeElementVisitor.VisitorKind.AGGREGATING, visitor.getVisitorKind());

        MyBatisMapperScanReflectionVisitor reflectionVisitor = new MyBatisMapperScanReflectionVisitor();
        assertEquals(Set.of("*"), reflectionVisitor.getSupportedAnnotationNames());
        assertEquals(TypeElementVisitor.VisitorKind.ISOLATING, reflectionVisitor.getVisitorKind());
        assertTrue(reflectionVisitor.getOrder() > GraalTypeElementVisitor.POSITION,
            "must run before the GraalTypeElementVisitor so that the @ReflectionConfig values are picked up");
    }

    private static Compilation compile(Path temporaryDirectory, List<JavaFileObject> sources) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path classes = temporaryDirectory.resolve("classes");
        Path generatedSources = temporaryDirectory.resolve("generated-sources");
        Files.createDirectories(classes);
        Files.createDirectories(generatedSources);

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classes.toFile()));
            fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(generatedSources.toFile()));

            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                List.of("-classpath", System.getProperty("java.class.path")),
                null,
                sources
            );
            task.setProcessors(List.of(
                new TestTypeElementVisitorProcessor(TypeElementVisitor.VisitorKind.ISOLATING,
                    new MyBatisMapperScanReflectionVisitor(), new GraalTypeElementVisitor()),
                new TestTypeElementVisitorProcessor(TypeElementVisitor.VisitorKind.AGGREGATING,
                    new MyBatisMapperScanVisitor())
            ));
            boolean success = task.call();
            String messages = diagnostics.getDiagnostics().stream()
                .map(Diagnostic::toString)
                .collect(Collectors.joining(System.lineSeparator()));
            return new Compilation(success, messages, classes);
        }
    }

    private record Compilation(boolean success, String diagnostics, Path classes) {

        URLClassLoader classLoader() throws Exception {
            return new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
        }
    }

    /**
     * Loads the generated {@code $ReflectConfig} class of the given type and records what it registers.
     */
    private static RecordingReflectionContext reflectionConfiguration(URLClassLoader classLoader, String typeName) throws Exception {
        Class<?> configurerClass = classLoader.loadClass(typeName + GraalReflectionConfigurer.CLASS_SUFFIX);
        GraalReflectionConfigurer configurer = (GraalReflectionConfigurer) configurerClass.getDeclaredConstructor().newInstance();
        RecordingReflectionContext context = new RecordingReflectionContext(classLoader);
        configurer.configure(context);
        return context;
    }

    private static final class RecordingReflectionContext implements GraalReflectionConfigurer.ReflectionConfigurationContext {
        private final ClassLoader classLoader;
        private final Set<String> proxies = new LinkedHashSet<>();
        private final Set<String> types = new LinkedHashSet<>();
        private final Set<String> members = new LinkedHashSet<>();

        private RecordingReflectionContext(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Class<?> findClassByName(String name) {
            try {
                return classLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public void register(Class<?>... classes) {
            Arrays.stream(classes).map(Class::getName).forEach(types::add);
        }

        @Override
        public void register(Method... methods) {
            Arrays.stream(methods).map(m -> m.getDeclaringClass().getName() + "#" + m.getName()).forEach(members::add);
        }

        @Override
        public void register(Field... fields) {
            Arrays.stream(fields).map(f -> f.getDeclaringClass().getName() + "." + f.getName()).forEach(members::add);
        }

        @Override
        public void register(Constructor<?>... constructors) {
            Arrays.stream(constructors).map(c -> c.getDeclaringClass().getName() + "#<init>").forEach(members::add);
        }

        @Override
        public void registerDynamicProxy(Class<?>... interfaces) {
            proxies.add(Arrays.stream(interfaces).map(Class::getName).collect(Collectors.joining(",")));
        }
    }

    private static final class TestTypeElementVisitorProcessor extends TypeElementVisitorProcessor {
        private final TypeElementVisitor.VisitorKind kind;
        private final List<TypeElementVisitor<?, ?>> visitors;

        private TestTypeElementVisitorProcessor(TypeElementVisitor.VisitorKind kind, TypeElementVisitor<?, ?>... visitors) {
            this.kind = kind;
            this.visitors = List.of(visitors);
        }

        @Override
        protected Collection<? extends TypeElementVisitor<?, ?>> findTypeElementVisitors() {
            return visitors;
        }

        @Override
        protected TypeElementVisitor.VisitorKind getIncrementalProcessorKind() {
            return kind;
        }
    }

    private static final class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String source;

        private InMemoryJavaFileObject(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
