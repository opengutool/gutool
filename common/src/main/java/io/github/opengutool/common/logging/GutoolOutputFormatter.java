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
package io.github.opengutool.common.logging;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 统一输出格式化工具类
 * 提供标准化的日志输出格式
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/25
 */
public class GutoolOutputFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 格式化带时间戳的日志输出
     */
    public static String formatWithTimestamp(String message) {
        if (StrUtil.isBlank(message)) {
            return "";
        }
        return DateUtil.format(new Date(), TIMESTAMP_FORMATTER) + " " + message.trim() + "\r\n";
    }

    /**
     * 格式化分隔线
     */
    public static String formatSeparator(String type) {
        return formatWithTimestamp("-----" + type + "-----");
    }

    /**
     * 格式化开始标记
     */
    public static String formatStartMarker() {
        return formatSeparator("start");
    }

    /**
     * 格式化结束标记
     */
    public static String formatEndMarker() {
        return formatSeparator("end");
    }

    /**
     * 格式化错误标记
     */
    public static String formatErrorMarker() {
        return formatSeparator("error");
    }

    /**
     * 格式化结果前缀
     */
    public static String formatResultPrefix(String content) {
        return "result:\n" + content + "\n";
    }

    /**
     * 格式化HTTP请求日志
     */
    public static String formatHttpLog(String method, String path, String message) {
        return String.format("[ %s %s ]%s", method, path, message);
    }
}
