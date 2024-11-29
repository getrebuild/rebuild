package com.rebuild.core.rbstore;

import com.rebuild.TestSupport;
import com.rebuild.core.support.task.TaskExecutors;
import org.junit.jupiter.api.Test;

import java.io.File;

class RbSystemImporterTest extends TestSupport {

    @Test
    void testImport() {
        RbSystemImporter importer = new RbSystemImporter(new File("D:/__RBSPKG"));
        TaskExecutors.run(importer);
    }
}