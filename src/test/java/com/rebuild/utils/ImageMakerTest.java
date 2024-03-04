package com.rebuild.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 */
class ImageMakerTest {

    @SuppressWarnings("deprecation")
    @Test
    void makeLogo() {
        File tmp = new File(FileUtils.getTempDirectory(), "logo.png");
        ImageMaker.makeLogo("锐昉科技", null, tmp);
        System.out.println(tmp);
    }

    @Test
    void makeAvatar() {
    }
}