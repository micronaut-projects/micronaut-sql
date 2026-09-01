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
import io.micronaut.configuration.mybatis.MyBatisMapperScan;
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
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisMapperScanVisitorTest {

    @Test
    void generatesRegistrationForDiscoveredMappers(@TempDir Path temporaryDirectory) throws Exception {
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
                List.of(new InMemoryJavaFileObject("example.config.MapperConfiguration", """
                    package example.config;

                    import io.micronaut.configuration.mybatis.MyBatisMapperScan;

                    @MyBatisMapperScan("example.mappers")
                    interface MapperConfiguration {
                        void customize(org.apache.ibatis.session.Configuration configuration);
                    }
                    """), new InMemoryJavaFileObject("example.mappers.GenreMapper", """
                    package example.mappers;

                    import org.apache.ibatis.annotations.Select;

                    public interface GenreMapper {
                        @Select("select 1")
                        int findOne();
                    }
                    """))
            );
            task.setProcessors(List.of(new TestTypeElementVisitorProcessor()));

            assertTrue(task.call(), diagnosticsToString(diagnostics));
            Path generatedRegistration = classes.resolve(
                "example/mappers/MyBatisMapperScanRegistration_example_config_MapperConfiguration.class"
            );
            assertTrue(Files.exists(generatedRegistration));
            try (URLClassLoader classLoader = new URLClassLoader(
                new java.net.URL[]{classes.toUri().toURL()},
                getClass().getClassLoader()
            )) {
                List<MyBatisMapperScanRegistration> registrations = new ArrayList<>();
                SoftServiceLoader.load(MyBatisMapperScanRegistration.class, classLoader).collectAll(registrations);

                assertEquals(1, registrations.size());
                MyBatisMapperScanRegistration registration = registrations.get(0);
                assertEquals("example.config.MapperConfiguration", registration.getCustomizerType());
                Configuration configuration = new Configuration();
                registration.register(configuration);
                assertTrue(configuration.hasMapper(classLoader.loadClass("example.mappers.GenreMapper")));
            }
        }
    }

    @Test
    void declaresSupportedAnnotationAndAggregatingKind() {
        MyBatisMapperScanVisitor visitor = new MyBatisMapperScanVisitor();

        assertEquals(
            Set.of(MyBatisMapperScan.class.getName()),
            visitor.getSupportedAnnotationNames()
        );
        assertEquals(TypeElementVisitor.VisitorKind.AGGREGATING, visitor.getVisitorKind());
    }

    @Test
    void scanUsesArrayContentsForEqualityAndStringValues() {
        MyBatisMapperScanVisitor.Scan scan = new MyBatisMapperScanVisitor.Scan(
            "example.config.MapperConfiguration",
            new String[]{"example.mappers"},
            "default"
        );
        MyBatisMapperScanVisitor.Scan equalScan = new MyBatisMapperScanVisitor.Scan(
            "example.config.MapperConfiguration",
            new String[]{"example.mappers"},
            "default"
        );
        MyBatisMapperScanVisitor.Scan differentScan = new MyBatisMapperScanVisitor.Scan(
            "example.config.MapperConfiguration",
            new String[]{"example.other"},
            "default"
        );

        assertEquals(scan, scan);
        assertEquals(scan, equalScan);
        assertEquals(scan.hashCode(), equalScan.hashCode());
        assertEquals(
            "Scan[elementName=example.config.MapperConfiguration, packages=[example.mappers], datasource=default]",
            scan.toString()
        );
        assertNotEquals(scan, differentScan);
        assertNotEquals(null, scan);
        assertNotEquals("not a scan", scan);
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

    private String diagnosticsToString(DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics().stream()
            .map(Diagnostic::toString)
            .reduce("", (left, right) -> left + System.lineSeparator() + right);
    }

    private static final class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String source;

        private InMemoryJavaFileObject(String className, String source) {
            super(java.net.URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
