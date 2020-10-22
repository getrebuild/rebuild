/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;

/**
 */
public class Tests {

    @Test
    public void testLocale() {
        for (Locale l : Locale.getAvailableLocales()) System.out.println(l.toString());
        System.out.println(Arrays.toString(Locale.getISOCountries()));
        System.out.println(Arrays.toString(Locale.getISOLanguages()));
    }
}
