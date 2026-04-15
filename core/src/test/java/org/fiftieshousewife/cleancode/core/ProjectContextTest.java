package org.fiftieshousewife.cleancode.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectContextTest {

    @Test
    void projectContextFieldsAccessible() {
        final ProjectContext ctx = new ProjectContext(
                Path.of("/project"),
                "my-project",
                "1.0.0",
                "21",
                List.of(Path.of("src/main/java")),
                List.of(Path.of("src/test/java")),
                Path.of("build"),
                Path.of("build/reports"),
                List.of()
        );

        assertEquals(Path.of("/project"), ctx.projectRoot());
        assertEquals("my-project", ctx.projectName());
        assertEquals("1.0.0", ctx.projectVersion());
        assertEquals("21", ctx.javaVersion());
        assertEquals(List.of(Path.of("src/main/java")), ctx.sourceRoots());
        assertEquals(List.of(Path.of("src/test/java")), ctx.testSourceRoots());
        assertEquals(Path.of("build"), ctx.buildDir());
        assertEquals(Path.of("build/reports"), ctx.reportsDir());
        assertEquals(List.of(), ctx.dependencies());
    }
}
