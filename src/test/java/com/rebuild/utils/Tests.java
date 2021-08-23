/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

/**
 */
public class Tests {

    @Test
    void test() {
        String patt = "((\\([0-9]{1,5}\\))?([0-9]{3,4}-)?[0-9]{7,8}(-[0-9]{2,6})?)|(1[356789][0-9]{9})";

        System.out.println(Pattern.matches(patt, "1234567"));
        System.out.println(Pattern.matches(patt, "1234567-123456"));
        System.out.println(Pattern.matches(patt, "123-1234567-123456"));
        System.out.println(Pattern.matches(patt, "(86)123-1234567-123456"));
        System.out.println(Pattern.matches(patt, "13712345678"));
        System.out.println(Pattern.matches(patt, "12712345678"));
        System.out.println(Pattern.matches(patt, "1371234567"));
    }
}
