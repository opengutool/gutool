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
package io.github.opengutool.common.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import io.github.opengutool.common.constants.GutoolExceptionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一异常处理工具类
 * 提供标准化的异常处理、格式化和日志记录功能
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/25
 */
public class GutoolExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GutoolExceptionHandler.class);

    /**
     * 格式化异常为简短堆栈信息（用于HTTP响应等）
     */
    public static String formatShortException(Exception e) {
        return ExceptionUtil.stacktraceToString(e, GutoolExceptionConstants.HTTP_STACK_TRACE_LIMIT);
    }

    /**
     * 格式化异常为中等长度堆栈信息（用于数据库存储等）
     */
    public static String formatMediumException(Exception e) {
        return ExceptionUtil.stacktraceToString(e, GutoolExceptionConstants.DB_STACK_TRACE_LIMIT);
    }

    /**
     * 格式化异常为完整堆栈信息（用于控制台输出等）
     */
    public static String formatFullException(Exception e) {
        return ExceptionUtil.stacktraceToString(e, GutoolExceptionConstants.CONSOLE_STACK_TRACE_LIMIT);
    }

    /**
     * 记录异常日志并返回格式化信息
     */
    public static String logAndFormatException(Exception e, String context) {
        logger.error("Exception in {}: {}", context, e.getMessage(), e);
        return formatMediumException(e);
    }

    /**
     * 记录异常日志并返回简短格式信息
     */
    public static String logAndFormatShortException(Exception e, String context) {
        logger.error("Exception in {}: {}", context, e.getMessage(), e);
        return formatShortException(e);
    }

    /**
     * 处理异常并执行回调操作
     */
    public static void handleException(Exception e, String context, ExceptionCallback callback) {
        String errorInfo = logAndFormatException(e, context);
        if (callback != null) {
            callback.onError(errorInfo);
        }
    }

    /**
     * 异常回调接口
     */
    @FunctionalInterface
    public interface ExceptionCallback {
        void onError(String errorMessage);
    }
}
