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
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.dictionary.py.PinyinDictionary;
import com.hankcs.hanlp.utility.TextUtility;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.RebuildException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
                .replaceAll("(?i)<script", "")
                .replaceAll("(?i)</script>", "")
                .replaceAll("(?i)<style", "")
                .replaceAll("(?i)</style>", "")
                .replaceAll("(?i)<iframe", "")
                .replaceAll("(?i)<img", "");
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
     * 打印调用栈 `dev only`
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
                if (args[i] == null) {
                    log.warn("{} argument [{}] is null", desc, i);
                    args[i] = new Object();
                }
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
        if (StringUtils.isBlank(filepath)) return;
        if (filepath.contains("../") || filepath.contains("..\\")
                || filepath.contains("<") || filepath.contains(">")) {
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
        if (source.length() == 4 || source.contains("-") || source.contains("年")) {
            source = source.replaceAll("[年月日\\-\\s:.]", "");
            String format = "yyyyMMddHHmmssSSS".substring(0, source.length());
            Date d = CalendarUtils.parse(source, format);
            if (d != null) return d;
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

    /**
     * 转为数组
     *
     * @param o
     * @return
     */
    public static Object[] toArray(Object o) {
        if (o == null) return new Object[0];
        if (o instanceof Object[]) return (Object[]) o;
        if (o instanceof Collection) return ((Collection<?>) o).toArray();

        if (o instanceof Iterable) {
            List<Object> c = new ArrayList<>();
            for (Object item : (Iterable<?>) o) c.add(item);
            return c.toArray();
        }
        return new Object[]{o};
    }

    /**
     * 转换成为拼音
     *
     * @param text
     * @param firstChar
     * @return
     */
    public static String convertToPinyinString(String text, boolean firstChar) {
        StringBuilder res = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (TextUtility.isChinese(c)) {
                List<Pinyin> pys = PinyinDictionary.convertToPinyin(String.valueOf(c), false);
                if (!pys.isEmpty()) {
                    String py = pys.get(0).getPinyinWithoutTone();
                    res.append(firstChar ? py.charAt(0) : py);
                }
            } else if (Character.isDigit(c) || Character.isLetter(c)) {
                res.append(c);
            }
        }
        // 只保留字母、数字、-
        return res.toString().replaceAll("[^a-zA-Z0-9\\-]", "").toUpperCase();
    }

    /**
     * @param prefix
     * @param hasHex
     * @return
     */
    public static String genPrettyName(String prefix, boolean hasHex) {
        String name = String.format("%s-%s",
                prefix == null ? "RB" : prefix,
                CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()));
        if (hasHex) name += "-" + CommonsUtils.randomHex().split("-")[0];
        return name;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName
     * @return
     * @see Files#probeContentType(Path)
     */
    public static String getFileExtension(String fileName) {
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(Paths.get(fileName));
        } catch (IOException ignored) {}

        if (mimeType == null && fileName.contains(".")) {
            return fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1);
        }
        return mimeType == null ? "unknown" : mimeType.split("/")[1];
    }

    /**
     * @param base64Data
     * @param dest
     */
    public static void base64ToFile(String base64Data, File dest) {
        byte[] decodedBytes = Base64.decodeBase64(base64Data);

        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(decodedBytes);
        } catch (IOException e) {
            throw new RebuildException(e);
        }
    }

    /**
     * @param file
     * @return
     */
    public static String fileToBase64(File file) {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return Base64.encodeBase64String(fileBytes);
        } catch (IOException e) {
            throw new RebuildException(e);
        }
    }

    /**
     * @param ex
     * @return
     */
    public static String getRootMessage(Throwable ex) {
        String msg = null;
        Throwable th = ExceptionUtils.getRootCause(ex);
        if (th != null) {
            msg = th.getMessage();
            if (StringUtils.isBlank(msg)) msg = ClassUtils.getShortClassName(th, "");
        }
        return msg == null ? "" : msg;
    }

    /**
     * 是否图片
     *
     * @param file
     * @return
     */
    public static boolean isImageFile(File file) {
        String filename = file.getName().toLowerCase();
        return  filename.endsWith(".gif")
                || filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".bmp");
    }

    /**
     * 是否 Office
     *
     * @param file
     * @return
     */
    public static boolean isOfficeFile(File file) {
        String filename = file.getName().toLowerCase();
        return  filename.endsWith(".doc")
                || filename.endsWith(".docx")
                || filename.endsWith(".xls")
                || filename.endsWith(".xlsx")
                || filename.endsWith(".ppt")
                || filename.endsWith(".pptx");
    }
}
