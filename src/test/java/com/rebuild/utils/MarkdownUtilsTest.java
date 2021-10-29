package com.rebuild.utils;

import org.junit.jupiter.api.Test;

class MarkdownUtilsTest {

    @Test
    void render() {
        System.out.println(MarkdownUtils.render("> content"));
        System.out.println(MarkdownUtils.render(
                "上海锐昉科技有限公司 [沪ICP备20020345号-3](https://beian.miit.gov.cn/)" +
                        "<script>alert(1)</script>" +
                        "<iframe url=\"http://baidu.com\"></iframe>"));
    }

    @Test
    void testExt() {
        // Table
        System.out.println(MarkdownUtils.render(
                "| Column 1 | Column 2 | Column 3 |\n" +
                "| -------- | -------- | -------- |\n" +
                "| Text     | Text     | Text     |"));

        // Tasklist
        System.out.println(MarkdownUtils.render("* [x] 123\n* [X] 456\n* [ ] 789"));
        System.out.println(MarkdownUtils.render("- [x] 123\n- [X] 456\n- [ ] 789"));
    }
}