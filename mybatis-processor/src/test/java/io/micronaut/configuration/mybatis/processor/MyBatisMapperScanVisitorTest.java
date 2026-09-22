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
import io.micronaut.core.io.service.SoftServiceLoader;
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
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
            new InMemoryJavaFileObject("example.mappers.Mappers", """
                package example.mappers;

                public final class Mappers {
                    public interface InnerMapper {
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
        assertTrue(Files.exists(compilation.classes().resolve(
            "example/config/MapperConfiguration$MyBatisMapperScanRegistration.class")));

        try (URLClassLoader classLoader = compilation.classLoader()) {
            List<MyBatisMapperScanRegistration> registrations = new ArrayList<>();
            SoftServiceLoader.load(MyBatisMapperScanRegistration.class, classLoader).collectAll(registrations);

            assertEquals(1, registrations.size());
            MyBatisMapperScanRegistration registration = registrations.get(0);
            assertEquals("orders", registration.getDatasourceName());

            Configuration configuration = new Configuration();
            registration.register(configuration);
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.GenreMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.nested.NestedMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers$InnerMapper")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers$Helper$DeepMapper")));
            assertFalse(configuration.hasMapper(classLoader.loadClass("example.mappers.Mappers")));
            assertTrue(configuration.hasMapper(classLoader.loadClass("example.other.OtherMapper")));
            assertFalse(configuration.hasMapper(classLoader.loadClass("example.other.NotScannedMapper")));

            // registering twice must not fail with a MyBatis "already known" error
            registration.register(configuration);
        }

        // no micronaut.processing.group/module options in this test: falls back to the annotated type's package
        Path nativeImage = compilation.classes().resolve("META-INF/native-image/example.config/mybatis-mapper-scan");
        String proxyConfig = Files.readString(nativeImage.resolve("proxy-config.json"));
        assertTrue(proxyConfig.contains("{\"interfaces\": [\"example.mappers.GenreMapper\"]}"), proxyConfig);
        assertTrue(proxyConfig.contains("example.mappers.Mappers$Helper$DeepMapper"), proxyConfig);
        assertTrue(proxyConfig.contains("example.other.OtherMapper"), proxyConfig);
        assertFalse(proxyConfig.contains("NotScannedMapper"), proxyConfig);

        String reflectConfig = Files.readString(nativeImage.resolve("reflect-config.json"));
        assertTrue(reflectConfig.contains("{\"name\": \"example.domain.Genre\", \"allDeclaredConstructors\": true"), reflectConfig);
        assertTrue(reflectConfig.contains("\"example.domain.Book\""), reflectConfig);
        assertFalse(reflectConfig.contains("java.lang.String"), reflectConfig);
        assertFalse(reflectConfig.contains("java.util"), reflectConfig);
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

        // nativeImageMetadata = false: no GraalVM metadata is generated
        assertFalse(Files.exists(compilation.classes().resolve("META-INF/native-image")));
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
            task.setProcessors(List.of(new TestTypeElementVisitorProcessor()));
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

    private static final class TestTypeElementVisitorProcessor extends TypeElementVisitorProcessor {
        @Override
        protected Collection<? extends TypeElementVisitor<?, ?>> findTypeElementVisitors() {
            return List.of(new MyBatisMapperScanVisitor());
        }

        @Override
        protected TypeElementVisitor.VisitorKind getIncrementalProcessorKind() {
            return TypeElementVisitor.VisitorKind.AGGREGATING;
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
