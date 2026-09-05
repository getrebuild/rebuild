/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.openai.core.JsonValue;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 思考过程提取器（模型无关）
 * <p>
 * 1) 结构化字段优先：扫描响应附加属性中的常见约定字段（如 DeepSeek 的 reasoning_content）
 * 2) &lt;think&gt; 标签兜底：部分部署将思考内容以 &lt;think&gt;...&lt;/think&gt; 包裹在正文开头
 *
 * @author Zixin
 * @since 2026/8/18
 */
public class ReasoningExtractor {

    // 常见思考内容字段，按优先级排列
    private static final String[] REASONING_KEYS = {"reasoning_content", "reasoning", "reasoning_text"};

    // 值为对象时尝试读取的子字段
    private static final String[] OBJECT_SUB_KEYS = {"text", "summary", "content"};

    /**
     * 从响应附加属性中提取思考内容
     *
     * @param additionalProps
     * @return
     */
    public static String fromProps(Map<String, JsonValue> additionalProps) {
        if (additionalProps == null || additionalProps.isEmpty()) return null;

        for (String key : REASONING_KEYS) {
            JsonValue value = additionalProps.get(key);
            if (value == null) continue;

            String s = optionalString(value.asString());
            if (StringUtils.isNotBlank(s)) return s;

            // 对象值（如 reasoning: {...}）尝试读取子字段
            @SuppressWarnings("unchecked")
            Map<String, JsonValue> objValue = (Map<String, JsonValue>) optionalOrNull(value.asObject());
            if (objValue != null) {
                for (String subKey : OBJECT_SUB_KEYS) {
                    JsonValue subValue = objValue.get(subKey);
                    if (subValue == null) continue;

                    String sub = optionalString(subValue.asString());
                    if (StringUtils.isNotBlank(sub)) return sub;
                }
            }
        }
        return null;
    }

    /**
     * SDK 方法编译后 descriptor 为裸 Optional，泛型推断不可靠，需显式取值
     *
     * @param optional
     * @return
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String optionalString(java.util.Optional<?> optional) {
        Object o = optional == null ? null : optional.orElse(null);
        return o == null ? null : String.valueOf(o);
    }

    /**
     * @param optional
     * @return
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Object optionalOrNull(java.util.Optional<?> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    /**
     * &lt;think&gt; 标签流式解析器
     * <p>
     * 仅当正文首个非空白内容为 &lt;think&gt; 时激活，将 &lt;/think&gt; 之前的内容剥离为思考内容；
     * 未激活或标签未出现时对内容无影响。标签可跨 chunk 边界。
     */
    public static class ThinkTagParser {

        private static final String THINK_OPEN = "<think>";
        private static final String THINK_CLOSE = "</think>";

        // 未激活：等待首个非空白内容以决定是否激活
        private static final int STATE_UNDETERMINED = 0;
        // 已激活：正在剥离思考内容，等待 </think>
        private static final int STATE_IN_THINK = 1;
        // 已结束：标签处理完毕，内容全部归正文
        private static final int STATE_DONE = 2;

        private int state = STATE_UNDETERMINED;

        // STATE_UNDETERMINED：已见空白/前缀缓冲；STATE_IN_THINK：疑似 </think> 的尾部缓冲
        private final StringBuilder buffer = new StringBuilder();

        /**
         * 输入一个内容 chunk，返回思考/正文拆分结果
         *
         * @param chunk
         * @return
         */
        public FeedResult feed(String chunk) {
            if (StringUtils.isEmpty(chunk)) return FeedResult.EMPTY;
            if (state == STATE_DONE) return new FeedResult(null, chunk);

            if (state == STATE_IN_THINK) {
                return feedInThink(chunk);
            }

            // STATE_UNDETERMINED
            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);
                if (Character.isWhitespace(c)) {
                    // 前导空白不影响激活判断，不缓冲
                    continue;
                }

                String pending = buffer.toString() + c;
                if (THINK_OPEN.startsWith(pending)) {
                    buffer.append(c);
                    if (buffer.length() == THINK_OPEN.length()) {
                        state = STATE_IN_THINK;
                        buffer.setLength(0);
                        // 标签后的剩余内容继续按思考内容处理
                        String rest = chunk.substring(i + 1);
                        return rest.isEmpty() ? FeedResult.EMPTY : feedInThink(rest);
                    }
                } else {
                    // 首内容非 <think>，永不激活，缓冲与后续内容全部归正文
                    state = STATE_DONE;
                    String content = pending + chunk.substring(i + 1);
                    return new FeedResult(null, content);
                }
            }
            return FeedResult.EMPTY;
        }

        /**
         * STATE_IN_THINK：查找 </think>，处理跨 chunk 边界
         *
         * @param chunk
         * @return
         */
        private FeedResult feedInThink(String chunk) {
            buffer.append(chunk);

            int closeIdx = buffer.indexOf(THINK_CLOSE);
            if (closeIdx >= 0) {
                String reasoning = buffer.substring(0, closeIdx);
                String rest = buffer.substring(closeIdx + THINK_CLOSE.length());
                state = STATE_DONE;
                buffer.setLength(0);
                return new FeedResult(reasoning, rest.isEmpty() ? null : rest);
            }

            // 尾部可能是不完整的 </think> 前缀，保留在缓冲中等待下一 chunk
            int keep = tailPrefixOverlap(buffer, THINK_CLOSE);
            String tail = buffer.substring(buffer.length() - keep);
            String reasoning = buffer.substring(0, buffer.length() - keep);
            buffer.setLength(0);
            buffer.append(tail);
            return reasoning.isEmpty() ? FeedResult.EMPTY : new FeedResult(reasoning, null);
        }

        /**
         * 轮次结束时冲刷尾部疑似 </think> 的缓冲（流式结束时调用，内容归入思考）
         *
         * @return
         */
        public String flushDangling() {
            if (state != STATE_IN_THINK || buffer.length() == 0) return null;
            String s = buffer.toString();
            buffer.setLength(0);
            return s;
        }

        /**
         * 后缀与前缀的最大重叠长度（如 "abc</th" 与 "</think" 重叠 4）
         *
         * @param buf
         * @param tag
         * @return
         */
        private static int tailPrefixOverlap(CharSequence buf, String tag) {
            int max = Math.min(buf.length(), tag.length() - 1);
            for (int len = max; len > 0; len--) {
                boolean match = true;
                for (int i = 0; i < len; i++) {
                    if (buf.charAt(buf.length() - len + i) != tag.charAt(i)) {
                        match = false;
                        break;
                    }
                }
                if (match) return len;
            }
            return 0;
        }
    }

    /**
     * 思考/正文拆分结果
     */
    public static class FeedResult {

        static final FeedResult EMPTY = new FeedResult(null, null);

        private final String reasoning;
        private final String content;

        FeedResult(String reasoning, String content) {
            this.reasoning = reasoning;
            this.content = content;
        }

        /**
         * @return 本 chunk 中的思考片段（可能为 null）
         */
        public String getReasoning() {
            return reasoning;
        }

        /**
         * @return 本 chunk 中的正文片段（可能为 null）
         */
        public String getContent() {
            return content;
        }

        /**
         * @return
         */
        public boolean isEmpty() {
            return reasoning == null && content == null;
        }
    }
}
