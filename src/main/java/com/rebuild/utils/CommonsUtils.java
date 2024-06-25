/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ReflectUtils;
import cn.devezhao.persist4j.engine.NullValue;
import cn.hutool.core.date.DateException;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.RebuildException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 通用工具类
 *
 * @author devezhao
 * @since 01/31/2019
 */
@Slf4j
public class CommonsUtils {

    // 打印开发级别日志
    public static final boolean DEVLOG = BootApplication.devMode();

    // 通用分隔符
    public static final String COMM_SPLITER = "$$$$";
    // 通用分隔符 REGEX
    public static final String COMM_SPLITER_RE = "\\$\\$\\$\\$";

    private static final Pattern PATT_PLAINTEXT = Pattern.compile("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+");

    private static final char[] SPECIAL_CHARS = "`~!@#$%^&*()_+=-{}|[];':\",./<>?".toCharArray();

    /**
     * 不含特殊字符。不允许除 `数字` `字母` `中文` `_` `-` 及空格以外的字符
     *
     * @param text
     * @return
     */
    public static boolean isPlainText(String text) {
        return !text.contains(" ") && PATT_PLAINTEXT.matcher(text).matches();
    }

    /**
     * 是否为特殊字符
     *
     * @param ch
     * @return
     */
    public static boolean isSpecialChar(char ch) {
        for (char c : SPECIAL_CHARS) {
            if (c == ch) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param text
     * @param maxLength
     * @return
     */
    public static String maxstr(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() > maxLength) return text.substring(0, maxLength);
        return text;
    }

    /**
     * @param text
     * @return
     * @see StringEscapeUtils#escapeHtml(String)
     */
    public static String escapeHtml(Object text) {
        if (text == null || StringUtils.isBlank(text.toString())) {
            return StringUtils.EMPTY;
        }

        // https://www.php.net/htmlspecialchars
        return text.toString()
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace(">", "&gt;")
                .replace("<", "&lt;");
    }

    /**
     * @param text
     * @return
     * @see #escapeHtml(Object)
     */
    public static String sanitizeHtml(Object text) {
        if (text == null || StringUtils.isBlank(text.toString())) {
            return StringUtils.EMPTY;
        }

        // TODO 更好的 sanitizeHtml
        return text.toString()
                .replace("<script", "")
                .replace("</script>", "")
                .replace("<style", "")
                .replace("</style>", "")
                .replace("<img", "");
    }

    /**
     * @param text
     * @return
     * @see StringEscapeUtils#escapeSql(String)
     */
    public static String escapeSql(Object text) {
        // https://github.com/getrebuild/rebuild/issues/594
        text = text.toString().replace("\\'", "'");
        // https://gitee.com/getrebuild/rebuild/issues/IA5G7U
        text = text.toString().replace("\\", "\\\\");
        return StringEscapeUtils.escapeSql((String) text);
    }

    /**
     * 获取 classpath 下的配置文件流
     *
     * @param file
     * @return
     * @see org.springframework.util.ResourceUtils#getFile(URI)
     */
    public static InputStream getStreamOfRes(String file) throws IOException {
        return new ClassPathResource(file).getInputStream();
    }

    /**
     * 获取 classpath 下的配置文件内容
     *
     * @param file
     * @return
     */
    public static String getStringOfRes(String file) {
        try (InputStream is = getStreamOfRes(file)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Cannot load file of res : {}", file);
            return null;
        }
    }

    /**
     * 随机 Hex(32)
     *
     * @return
     */
    public static String randomHex() {
        return randomHex(false);
    }

    /**
     * 随机 Hex(32)
     *
     * @param simple Remove `-`
     * @return
     */
    public static String randomHex(boolean simple) {
        String hex = UUID.randomUUID().toString();
        if (simple) hex = hex.replace("-", "");
        return hex;
    }

    /**
     * 指定范围的随机数
     *
     * @param s
     * @param e
     * @return
     * @see RandomUtils#nextInt(int)
     */
    public static int randomInt(int s, int e) {
        int rnd = RandomUtils.nextInt(e);
        return rnd < s ? rnd + s : rnd;
    }

    /**
     * 转整数（四舍五入）
     *
     * @param number
     * @return
     * @see cn.devezhao.commons.ObjectUtils#toLong(Object)
     */
    public static Long toLongHalfUp(Object number) {
        double doubleValue = ObjectUtils.toDouble(number);
        return BigDecimal.valueOf(doubleValue)
                .setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 打印调用栈（for DEBUG）
     */
    public static void printStackTrace() {
        if (Application.devMode() || log.isDebugEnabled()) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            for (StackTraceElement traceElement : trace) {
                System.err.println("\tat " + traceElement);
            }
        }
    }

    /**
     * @param strs
     * @param search
     * @return
     * @see StringUtils#containsIgnoreCase(String, String)
     */
    public static boolean containsIgnoreCase(String[] strs, String search) {
        for (String s : strs) {
            if (StringUtils.containsIgnoreCase(s, search)) return true;
        }
        return false;
    }

    /**
     * @param desc
     * @param args
     * @return
     */
    public static Object invokeMethod(String desc, Object... args) {
        String[] classAndMethod = desc.split("#");
        try {
            Class<?> clazz = ReflectUtils.classForName(classAndMethod[0]);

            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            Method method = clazz.getMethod(classAndMethod[1], paramTypes);
            return method.invoke(null, args);

        } catch (ReflectiveOperationException ex) {
            log.error("Invalid method invoke : {}", desc);
            throw new RebuildException(ex);
        }
    }

    /**
     * @param str
     * @return
     */
    public static boolean isExternalUrl(String str) {
        return str != null
                && (str.startsWith("http://") || str.startsWith("https://"));
    }

    /**
     * 判断任意对象是否不为空
     *
     * @param any
     * @return
     */
    public static boolean hasLength(Object any) {
        if (any == null) return false;
        if (any.getClass().isArray()) return ((Object[]) any).length > 0;
        if (any instanceof Collection) return !((Collection<?>) any).isEmpty();
        if (NullValue.is(any)) return false;
        return !any.toString().isEmpty();
    }

    /**
     * 值相等
     *
     * @param a
     * @param b
     * @return
     */
    @SuppressWarnings("unchecked")
    public static boolean isSame(Object a, Object b) {
        if (a == null && b != null) return false;
        if (a != null && b == null) return false;
        if (Objects.equals(a, b)) return true;

        // 数字
        if (a instanceof Number && b instanceof Number) {
            // FIXME 有精度问题
            return ObjectUtils.toDouble(a) == ObjectUtils.toDouble(b);
        }

        // 集合/数组
        if ((a instanceof Collection || a instanceof Object[]) && (b instanceof Collection || b instanceof Object[])) {
            Collection<Object> aColl;
            if (a instanceof Object[]) aColl = Arrays.asList((Object[]) a);
            else aColl = (Collection<Object>) a;
            Collection<Object> bColl;
            if (b instanceof Object[]) bColl = Arrays.asList((Object[]) b);
            else bColl = (Collection<Object>) b;

            if (aColl.size() != bColl.size()) return false;
            if (aColl.isEmpty()) return true;
            return CollectionUtils.containsAll(aColl, bColl) && CollectionUtils.containsAll(bColl, aColl);
        }

        // 其他
        // FIXME 完善不同值类型的比较
        return StringUtils.equals(a.toString(), b.toString());
    }

    /**
     * @param filepath
     * @throws SecurityException
     */
    public static void checkSafeFilePath(String filepath) throws SecurityException {
        if (filepath == null) return;
        if (filepath.contains("../") || filepath.contains("<") || filepath.contains(">")) {
            throw new SecurityException("Attack path detected : " + escapeHtml(filepath));
        }
    }

    /**
     * 日期转换
     *
     * @param source
     * @return
     */
    public static Date parseDate(String source) {
        if ("yyyy".length() == source.length()) {
            return CalendarUtils.parse(source, "yyyy");
        }
        if ("yyyy-MM".length() == source.length()) {
            return CalendarUtils.parse(source, "yyyy-MM");
        }

        try {
            DateTime dt = DateUtil.parse(source);
            if (dt != null) return dt.toJdkDate();
        } catch (DateException ignored) {
        }

        // 2017/11/19 11:07
        if (source.contains("/")) {
            String[] fs = new String[]{"yyyy/M/d H:m:s", "yyyy/M/d H:m", "yyyy/M/d"};
            for (String format : fs) {
                Date d = CalendarUtils.parse(source, format);
                if (d != null) return d;
            }
        }

        return null;
    }
}
