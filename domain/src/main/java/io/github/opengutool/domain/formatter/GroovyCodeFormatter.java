/*
 * Copyright © 2025/9/3 gutool (gutool@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.opengutool.domain.formatter;


import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GroovyCodeFormatter {
    public static String format(String input) {
        try {
            input = StrUtil.trim(input);
            if (StrUtil.isNotBlank(input)) {
                return new GroovyCodeFormat().format(input);
            }
            return input;
        } catch (Exception e) {
            throw new RuntimeException("Error formatting Java code", e);
        }
    }

    private static class GroovyCodeFormat {
        private String indentString;
        private Map<String, String> stringLiteralReplacements;
        private int replacementCounter;
        private int tabSize = 4;

        // Patterns for string literals
        private static final Pattern SINGLE_QUOTE_STRING = Pattern.compile("'([^'\\\\]|\\\\.)*'");
        private static final Pattern DOUBLE_QUOTE_STRING = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
        private static final Pattern TRIPLE_SINGLE_QUOTE_STRING = Pattern.compile("'''(.*?)'''", Pattern.DOTALL);
        private static final Pattern TRIPLE_DOUBLE_QUOTE_STRING = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
        private static final Pattern DOLLAR_SLASH_STRING = Pattern.compile("\\$/(.*?)/\\$", Pattern.DOTALL);
        private static final Pattern REGEX_PATTERN = Pattern.compile("/(?:[^/\\\\\\n]|\\\\.)*?/");

        public GroovyCodeFormat() {
            this.stringLiteralReplacements = new HashMap<>();
            this.replacementCounter = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tabSize; i++) {
                sb.append(" ");
            }
            this.indentString = sb.toString();
        }

        public String format(String code) {
            // Clear previous replacements
            stringLiteralReplacements.clear();
            replacementCounter = 0;

            // Extract and replace string literals to protect them
            code = extractStringLiterals(code);

            // First, split the code into statements/blocks
            code = splitIntoLines(code);

            StringBuilder formatted = new StringBuilder();
            String[] lines = code.split("\n", -1);
            int indentLevel = 0;
            boolean inMultilineComment = false;

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                String trimmed = line.trim();

                // Handle multiline comments
                if (trimmed.startsWith("/*")) {
                    inMultilineComment = true;
                }

                // Handle empty lines
                if (trimmed.isEmpty()) {
                    if (lineIndex < lines.length - 1) {
                        formatted.append("\n");
                    }
                    continue;
                }

                // Decrease indent for closing braces
                if (!inMultilineComment && (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")"))) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }

                // Add indentation
                for (int i = 0; i < indentLevel; i++) {
                    formatted.append(indentString);
                }

                // Format the line content
                String formattedLine = formatLine(trimmed);
                formatted.append(formattedLine);

                // Add newline except for last line if original didn't have one
                if (lineIndex < lines.length - 1 || code.endsWith("\n")) {
                    formatted.append("\n");
                }

                // Increase indent for opening braces
                if (!inMultilineComment && (endsWithOpenBrace(formattedLine))) {
                    indentLevel++;
                }

                // Handle multiline comments
                if (trimmed.endsWith("*/")) {
                    inMultilineComment = false;
                }
            }

            String result = formatted.toString();
            // Restore string literals
            return restoreStringLiterals(result);
        }

        private String splitIntoLines(String code) {
            // Don't split if already well-formatted (has proper line breaks)
            if (code.contains("\n")) {
                return code;
            }

            // Only split if it's a class, method or control structure
            if (code.matches(".*\\b(class|interface|trait|def|if|while|for)\\b.*\\{.*\\}.*")) {
                // Split single-line code blocks into multiple lines
                // But be careful with closures and one-liners
                code = code.replaceAll("\\{\\s*\\}", "{ }"); // Keep empty blocks on same line
                code = code.replaceAll("\\{(?!\\s*\\})", "{\n");
                code = code.replaceAll("(?<!\\{\\s{0,10})\\}", "\n}");
                code = code.replaceAll(";(?!\\s*$)", ";\n");

                // Clean up multiple newlines
                code = code.replaceAll("\n{3,}", "\n\n");
            }

            return code.trim();
        }

        private String formatLine(String line) {
            // Special handling for import statements
            if (line.startsWith("import") || line.startsWith("package")) {
                return line.trim();
            }

            // Handle safe navigation and Elvis operator first
            line = line.replaceAll("\\s*\\?\\.\\s*", "?.");
            line = line.replaceAll("\\s*\\?:\\s*", " ?: ");

            // Handle spread operator
            line = line.replaceAll("\\s*\\.\\*\\s*", ".*");

            // First, preserve -> by replacing it with a placeholder
            String arrowPlaceholder = "__GROOVY_ARROW__";
            line = line.replaceAll("->", arrowPlaceholder);

            // Handle various operators - but do compound operators first
            line = line.replaceAll("\\s*==\\s*", " == ");
            line = line.replaceAll("\\s*!=\\s*", " != ");
            line = line.replaceAll("\\s*<=\\s*", " <= ");
            line = line.replaceAll("\\s*>=\\s*", " >= ");
            line = line.replaceAll("\\s*\\+=\\s*", " += ");
            line = line.replaceAll("\\s*-=\\s*", " -= ");
            line = line.replaceAll("\\s*\\*=\\s*", " *= ");
            line = line.replaceAll("\\s*/=\\s*", " /= ");

            // Now handle individual operators
            line = line.replaceAll("\\s*=\\s*", " = ");
            line = line.replaceAll("\\s*<\\s*", " < ");
            line = line.replaceAll("\\s*>\\s*", " > ");

            // Restore -> with proper spacing
            line = line.replaceAll(arrowPlaceholder, " -> ");

            // Add spaces around braces, but handle closure syntax specially
            line = line.replaceAll("\\s*\\{\\s*", " {");
            line = line.replaceAll("\\s*\\}\\s*", "}");

            // Handle parentheses
            line = line.replaceAll("\\s*\\(\\s*", "(");
            line = line.replaceAll("\\s*\\)\\s*", ")");

            // Handle method calls and definitions
            line = line.replaceAll("\\)\\s*\\{", ") {");

            // Handle commas and semicolons
            line = line.replaceAll("\\s*,\\s*", ", ");
            line = line.replaceAll("\\s*;\\s*", "; ");

            // Handle colons in maps and labels
            line = line.replaceAll("\\s*:\\s*", ": ");

            // Handle class, interface, trait definitions
            if (line.matches("^(class|interface|trait|enum)\\s+.*")) {
                line = line.replaceAll("(class|interface|trait|enum)\\s+", "$1 ");
            }

            // Clean up multiple spaces
            line = line.replaceAll("\\s+", " ");

            return line.trim();
        }

        private String extractStringLiterals(String code) {
            // Order matters: extract larger patterns first to avoid nested extraction
            code = extractPattern(code, TRIPLE_DOUBLE_QUOTE_STRING);
            code = extractPattern(code, TRIPLE_SINGLE_QUOTE_STRING);
            code = extractPattern(code, DOLLAR_SLASH_STRING);

            // 按照遇到的顺序提取字符串，确保匹配的引号配对
            code = extractStringsByOrder(code);

            code = extractPattern(code, REGEX_PATTERN);
            return code;
        }

        private String extractStringsByOrder(String code) {
            StringBuilder result = new StringBuilder();
            int i = 0;

            while (i < code.length()) {
                char c = code.charAt(i);

                if (c == '"') {
                    // 处理双引号字符串
                    int start = i;
                    i = findStringEnd(code, i, '"');
                    if (i != -1) {
                        String str = code.substring(start, i + 1);
                        String placeholder = "__STRING_LITERAL_" + (replacementCounter++) + "__";
                        stringLiteralReplacements.put(placeholder, str);
                        result.append(placeholder);
                    } else {
                        result.append(c);
                        i = start;
                    }
                } else if (c == '\'') {
                    // 处理单引号字符串
                    int start = i;
                    i = findStringEnd(code, i, '\'');
                    if (i != -1) {
                        String str = code.substring(start, i + 1);
                        String placeholder = "__STRING_LITERAL_" + (replacementCounter++) + "__";
                        stringLiteralReplacements.put(placeholder, str);
                        result.append(placeholder);
                    } else {
                        result.append(c);
                        i = start;
                    }
                } else {
                    result.append(c);
                }
                i++;
            }

            return result.toString();
        }

        private int findStringEnd(String code, int start, char quote) {
            int i = start + 1;
            while (i < code.length()) {
                char c = code.charAt(i);
                if (c == quote) {
                    return i;
                } else if (c == '\\' && i + 1 < code.length()) {
                    i++; // 跳过转义字符
                }
                i++;
            }
            return -1; // 没有找到匹配的结束引号
        }

        private String extractPattern(String code, Pattern pattern) {
            Matcher matcher = pattern.matcher(code);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String placeholder = "__STRING_LITERAL_" + (replacementCounter++) + "__";
                stringLiteralReplacements.put(placeholder, matcher.group());
                matcher.appendReplacement(sb, placeholder);
            }
            matcher.appendTail(sb);

            return sb.toString();
        }

        private String restoreStringLiterals(String code) {
            for (Map.Entry<String, String> entry : stringLiteralReplacements.entrySet()) {
                code = code.replace(entry.getKey(), entry.getValue());
            }
            return code;
        }

        private boolean endsWithOpenBrace(String line) {
            // Check if line ends with opening brace, but not inside a string literal placeholder
            String trimmed = line.trim();
            if (trimmed.contains("__STRING_LITERAL_") && trimmed.endsWith("__")) {
                return false;
            }
            return trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.endsWith("(");
        }
    }
}
