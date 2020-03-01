/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.language;

import com.rebuild.server.TestSupport;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * @author devezhao
 * @since 11/29/2019
 */
public class LanguagesTest extends TestSupport {

    @Test
    public void getBundle() {
        System.out.println(Languages.instance.getDefaultBundle());
        System.out.println(Languages.instance.getCurrentBundle());
        System.out.println(Languages.instance.getBundle(Locale.getDefault()));

        assertEquals(Locale.US,
                Locale.forLanguageTag(Languages.instance.getBundle(Locale.US).locale()));
        assertEquals(Locale.JAPAN,
                Locale.forLanguageTag(Languages.instance.getBundle(Locale.JAPAN).locale()));
    }

    @Test
    public void lang() {
        System.out.println(Languages.lang("rebuild"));
        System.out.println(Languages.lang("rebuild-undef"));

        System.out.println(Languages.instance.getBundle(Locale.US).lang("rebuild"));
        System.out.println(Languages.instance.getBundle(Locale.JAPAN).lang("rebuild"));
        System.out.println(Languages.instance.getBundle(Locale.GERMAN).lang("rebuild"));
    }

    @Test
    public void langMerge() {
        System.out.println(Languages.lang("UsernameOrEmail"));
    }

    @Test
    public void langMD() {
        System.out.println(Languages.lang("SignupPending"));
        System.out.println(Languages.lang("RightsTip"));
        System.out.println(Languages.lang("RightsTip"));
    }
}