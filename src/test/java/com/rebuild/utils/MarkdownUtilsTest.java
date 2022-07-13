package com.rebuild.utils;

import org.junit.jupiter.api.Test;

class MarkdownUtilsTest {

    @Test
    void render() {
        System.out.println(MarkdownUtils.render("这是引用\n> content"));

        System.out.println(MarkdownUtils.render("你有 2 条日程提醒（注意要两个换行符）\n" +
                "\n- 123" +
                "\n- [456](/rebuild/789)"));

        System.out.println(MarkdownUtils.render(
                "测试 XSS 上海锐昉科技有限公司 [沪ICP备20020345号-3](https://beian.miit.gov.cn/)" +
                        "<script>alert(1)</script>" +
                        "<iframe url=\"http://baidu.com\"></iframe>"));
    }

    @Test
    void cleanMd() {
        System.out.println(MarkdownUtils.cleanMd("你有 2 条日程提醒（注意要两个换行符）\n" +
                "\n- 123" +
                "\n- [456](/rebuild/789)"));
    }

    @Test
    void testExt() {
        // Table
        System.out.println(MarkdownUtils.render(
                "这是表格（注意要两个换行符）\n\n" +
                "| Column 1 | Column 2 | Column 3 |\n" +
                "| -------- | -------- | -------- |\n" +
                "| Text     | Text     | Text     |"));

        // Tasklist
        System.out.println(MarkdownUtils.render(
                "这是任务列表（注意要两个换行符）\n\n- [x] 123\n- [X] 456\n- [ ] 789"));
        System.out.println(MarkdownUtils.render(
                "* [x] 123\n* [X] 456\n* [ ] 789"));
    }
}