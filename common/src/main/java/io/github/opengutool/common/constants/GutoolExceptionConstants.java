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
package io.github.opengutool.common.constants;

/**
 * 异常处理相关常量
 * 统一管理异常堆栈跟踪长度限制等魔数
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/25
 */
public final class GutoolExceptionConstants {

    private GutoolExceptionConstants() {
        // 工具类，禁止实例化
    }

    /**
     * HTTP响应异常堆栈跟踪长度限制
     */
    public static final int HTTP_STACK_TRACE_LIMIT = 500;

    /**
     * 数据库存储异常堆栈跟踪长度限制
     */
    public static final int DB_STACK_TRACE_LIMIT = 1000;

    /**
     * 控制台输出异常堆栈跟踪长度限制（无限制）
     */
    public static final int CONSOLE_STACK_TRACE_LIMIT = Integer.MAX_VALUE;

    /**
     * 默认线程池大小
     */
    public static final int DEFAULT_THREAD_POOL_SIZE = 5;

    /**
     * HTTP请求最大内容长度（10MB）
     */
    public static final long MAX_HTTP_CONTENT_LENGTH = 10 * 1024 * 1024L;
}
