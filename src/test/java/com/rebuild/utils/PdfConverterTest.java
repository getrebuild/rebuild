package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

class PdfConverterTest {

    @Test
    void convert() throws IOException {
        PdfConverter.convert(Paths.get("D:\\GitHub\\for-production\\rebuild-market\\202211东航\\功能清单及工时_20221110.xlsx"), true);
    }
}