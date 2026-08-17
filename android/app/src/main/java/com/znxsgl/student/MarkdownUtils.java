package com.znxsgl.student;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 纯文本课时内容的兜底格式化：自动拆段、盘古之白、识别代码表达式并正确标记。
 * 已包含 Markdown 标记的内容原样返回，避免二次处理。
 */
public class MarkdownUtils {

    // 判断是否已经具备 Markdown 结构
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile(
            "^#{1,6}\\s|^```|^\\*\\*|^`|^\\s*[-*+]|^\\s*\\d+\\.",
            Pattern.MULTILINE
    );

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "def", "class", "for", "while", "if", "else", "elif", "import", "from",
            "return", "lambda", "try", "except", "with", "as", "print", "len", "range",
            "sum", "max", "min", "sorted", "enumerate", "zip", "map", "filter",
            // 常见数学/科学计算函数
            "sin", "cos", "tan", "exp", "log", "sqrt", "abs", "pow", "mean", "std",
            // 常见库/工具名
            "numpy", "pandas", "matplotlib", "python", "java", "json", "sql"
    ));

    /**
     * 仅在内容不含 Markdown 标记时进行自动格式化。
     */
    public static String autoFormatIfNeeded(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        if (MARKDOWN_PATTERN.matcher(content).find()) {
            return content;
        }

        String text = content.trim();

        // 1. 盘古之白：中英文/数字之间加空格
        text = panguSpacing(text);

        // 2. 识别并包裹代码表达式（行内代码或独立代码块）
        text = processCodeExpressions(text);

        // 3. 按中文/英文句末标点拆段
        text = text.replaceAll("([。；！？;!?.])([^\\s])", "$1\n\n$2");

        return text;
    }

    private static String panguSpacing(String text) {
        String spaced = text.replaceAll("([\\u4e00-\\u9fa5])([A-Za-z0-9])", "$1 $2");
        spaced = spaced.replaceAll("([A-Za-z0-9])([\\u4e00-\\u9fa5])", "$1 $2");
        return spaced;
    }

    /**
     * 扫描文本，识别代码表达式并替换为 Markdown 行内代码或 fenced code block。
     * 支持带平衡括号/方括号的嵌套表达式，如 np.sqrt(np.sum((a-b)**2))。
     */
    private static String processCodeExpressions(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = text.length();
        while (i < len) {
            CodeMatch match = findCodeExpression(text, i);
            if (match != null) {
                String code = match.code;
                if (shouldBeBlock(code)) {
                    result.append("\n\n```\n").append(code).append("\n```\n\n");
                } else {
                    result.append("`").append(code).append("`");
                }
                i = match.end;
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    private static CodeMatch findCodeExpression(String text, int start) {
        int len = text.length();
        int i = start;
        if (i >= len || !Character.isLetter(text.charAt(i))) {
            return null;
        }

        // 读取标识符链，如 np.sqrt、JAVA_HOME、System.out.println
        while (i < len) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                i++;
            } else {
                break;
            }
        }
        if (i == start) return null;

        String prefix = text.substring(start, i);

        // 判断这串字符是否像代码
        boolean looksLikeCode = prefix.contains(".") || isAllUpperSnake(prefix) || KEYWORDS.contains(prefix.toLowerCase());

        int end = i;
        // 处理数组索引 [...]
        if (i < len && text.charAt(i) == '[') {
            int close = findMatching(text, i, '[', ']');
            if (close != -1) {
                end = close + 1;
                looksLikeCode = true;
            }
        }
        // 处理函数调用 (...)
        if (i < len && text.charAt(i) == '(') {
            int close = findMatching(text, i, '(', ')');
            if (close != -1) {
                end = close + 1;
                looksLikeCode = true;
            }
        }

        if (!looksLikeCode) return null;

        String code = text.substring(start, end);
        return new CodeMatch(code, end);
    }

    private static int findMatching(String text, int openPos, char open, char close) {
        int depth = 0;
        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static boolean shouldBeBlock(String code) {
        // 含空格、包含嵌套括号、长度超过阈值，或包含换行符 → 独立代码块
        if (code.contains("\n")) return true;
        if (code.contains(" ")) return true;
        if (code.length() > 18) return true;
        int parenDepth = 0;
        boolean hasNested = false;
        for (char c : code.toCharArray()) {
            if (c == '(') {
                parenDepth++;
                if (parenDepth > 1) hasNested = true;
            } else if (c == ')') {
                parenDepth--;
            }
        }
        return hasNested;
    }

    private static boolean isAllUpperSnake(String text) {
        if (text.length() < 2) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '_' && !Character.isUpperCase(c) && !Character.isDigit(c)) {
                return false;
            }
        }
        return text.contains("_");
    }

    private static boolean isKeyword(String token) {
        return KEYWORDS.contains(token.toLowerCase());
    }

    private static class CodeMatch {
        final String code;
        final int end;

        CodeMatch(String code, int end) {
            this.code = code;
            this.end = end;
        }
    }
}
