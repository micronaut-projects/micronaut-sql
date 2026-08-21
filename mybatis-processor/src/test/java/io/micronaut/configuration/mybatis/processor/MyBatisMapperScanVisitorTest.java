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
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisMapperScanVisitorTest {

    @Test
    void addsDiscoveredMappersToAnnotationMetadata(@TempDir Path temporaryDirectory) throws Exception {
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
            TestTypeElementVisitorProcessor processor = new TestTypeElementVisitorProcessor();
            task.setProcessors(List.of(processor));

            assertTrue(task.call(), diagnosticsToString(diagnostics));
            assertEquals(List.of("example.mappers.GenreMapper"), processor.mapperNames);
        }
    }

    private static final class TestTypeElementVisitorProcessor extends TypeElementVisitorProcessor {
        private final MapperMetadataVisitor mapperMetadataVisitor = new MapperMetadataVisitor();

        @Override
        protected Collection<? extends TypeElementVisitor<?, ?>> findTypeElementVisitors() {
            return List.of(new MyBatisMapperScanVisitor(), mapperMetadataVisitor);
        }

        @Override
        protected TypeElementVisitor.VisitorKind getIncrementalProcessorKind() {
            return TypeElementVisitor.VisitorKind.AGGREGATING;
        }

        private final List<String> mapperNames = mapperMetadataVisitor.mapperNames;
    }

    private static final class MapperMetadataVisitor implements TypeElementVisitor<Object, Object> {
        private final List<String> mapperNames = new java.util.ArrayList<>();
        private String configurationElementName;
        private boolean processed;

        @Override
        public void visitClass(ClassElement element, VisitorContext context) {
            if (element.getName().equals("example.config.MapperConfiguration")) {
                configurationElementName = element.getName();
            }
        }

        @Override
        public void finish(VisitorContext context) {
            if (processed) {
                return;
            }
            processed = true;
            ClassElement configurationElement = context.getClassElement(configurationElementName).orElseThrow();
            AnnotationValue<MyBatisMapperScan> scan = configurationElement.getAnnotation(MyBatisMapperScan.class);
            if (scan != null) {
                for (AnnotationClassValue<?> mapper : scan.annotationClassValues("mappers")) {
                    mapperNames.add(mapper.getName());
                }
            }
        }

        @Override
        public VisitorKind getVisitorKind() {
            return VisitorKind.AGGREGATING;
        }

        @Override
        public int getOrder() {
            return -100;
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
