/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.Test;

/**
 * https://www.jianshu.com/p/191d1e21f7ed
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/16
 */
public class MarkdownUtilsTest {

	@Test
	public void testRender() throws Exception {
	    render("hello" +
                "\n word");

        render("# Head");
        render("## Head");

        render("*A*");
        render("**A**");
        render("***A***");

        render("***");

        render("> P");
        render("> P" +
                ">> P" +
                "\n>>> P");

        render("`CODE`");
        render("```" +
                "\nCODE" +
                "\n```");

        render("[Click here](/admin/)");
        render("![alt](/img.png)");

        render("|COL1|COL2|COL3|"
                + "\n|---|---|---|"
                + "\n|data|data||"
                + "\n[Table 1. caption]");

        render("1. a" +
                "\n   1. a1" +
                "\n2. b");
	}

	private void render(String md) {
	    System.out.println("MD: ");
	    System.out.println(md);
	    System.out.println("HTML :");
	    System.out.println(MarkdownUtils.render(md));
    }
}
