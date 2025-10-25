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
package io.github.opengutool.domain.http;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.opengutool.common.constants.GutoolExceptionConstants;
import io.github.opengutool.common.exception.GutoolExceptionHandler;
import io.github.opengutool.common.logging.GutoolOutputFormatter;
import io.github.opengutool.domain.func.GutoolFunc;
import io.github.opengutool.domain.func.GutoolFuncContainer;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineHttp;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/20
 */
@Slf4j
public class GutoolHttpRequestHandler extends HttpServlet {

    private static final long MAX_CONTENT_LENGTH = GutoolExceptionConstants.MAX_HTTP_CONTENT_LENGTH;
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");

    private final GutoolFuncTabPanel panel;
    private final List<GutoolFuncTabPanelDefineHttp> httpConfigs;
    private final Consumer<String> outputCallback;
    private final Consumer<Object> resultHandler;

    public GutoolHttpRequestHandler(GutoolFuncTabPanel panel, List<GutoolFuncTabPanelDefineHttp> httpConfigs, Consumer<String> outputCallback, Consumer<Object> resultHandler) {
        this.panel = Objects.requireNonNull(panel, "面板配置不能为空");
        this.httpConfigs = httpConfigs != null ? Collections.synchronizedList(new ArrayList<>(httpConfigs)) : new ArrayList<>();
        this.outputCallback = outputCallback;
        this.resultHandler = resultHandler;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // 基本请求验证
            if (!validateBasicRequest(request, response)) {
                return;
            }

            // 查找匹配的HTTP配置
            GutoolFuncTabPanelDefineHttp matchedConfig = findMatchingConfig(request);
            if (matchedConfig == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        "未找到匹配的路由配置: " + request.getMethod() + " " + request.getRequestURI());
                return;
            }

            // 检查是否启用
            if (!Boolean.TRUE.equals(matchedConfig.getEnabled())) {
                sendErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "HTTP服务已禁用");
                return;
            }

            // 更新最后访问时间
            matchedConfig.setLastAccessTime(new Date());
            // 保存
            panel.sortHttpConfigs();
            // 处理请求
            processRequest(matchedConfig, request, response);

        } catch (Exception e) {
            log.error("处理HTTP请求时发生错误", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 基本请求验证
     */
    private boolean validateBasicRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 验证HTTP方法
        String method = request.getMethod();
        if (!ALLOWED_METHODS.contains(method)) {
            sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "不支持的HTTP方法: " + method);
            return false;
        }

        // 验证内容长度
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_CONTENT_LENGTH) {
            sendErrorResponse(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "请求体过大，最大允许: " + MAX_CONTENT_LENGTH + " 字节");
            return false;
        }

        // 验证Content-Type（对于有请求体的方法）
        if (Arrays.asList("POST", "PUT", "PATCH").contains(method) &&
                request.getContentType() == null && contentLength > 0) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "缺少Content-Type头");
            return false;
        }

        return true;
    }

    /**
     * 查找匹配的HTTP配置，优化匹配算法
     */
    private GutoolFuncTabPanelDefineHttp findMatchingConfig(HttpServletRequest request) {
        String method = request.getMethod();
        String path = normalizePath(request.getRequestURI());

        // 优先匹配精确路径
        for (GutoolFuncTabPanelDefineHttp config : httpConfigs) {
            if (Boolean.TRUE.equals(config.getEnabled()) &&
                    method.equalsIgnoreCase(config.getMethod()) &&
                    isExactPathMatch(path, config.getPath())) {
                return config;
            }
        }

        // 然后匹配通配符路径
        for (GutoolFuncTabPanelDefineHttp config : httpConfigs) {
            if (Boolean.TRUE.equals(config.getEnabled()) &&
                    method.equalsIgnoreCase(config.getMethod()) &&
                    isWildcardPathMatch(path, config.getPath())) {
                return config;
            }
        }

        return null;
    }

    /**
     * 标准化路径
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }
        return path.isEmpty() ? "/" : path;
    }

    /**
     * 精确路径匹配
     */
    private boolean isExactPathMatch(String requestPath, String configPath) {
        return Objects.equals(normalizePath(configPath), requestPath);
    }

    /**
     * 通配符路径匹配
     */
    private boolean isWildcardPathMatch(String requestPath, String configPath) {
        if (configPath == null) {
            return false;
        }

        String normalizedConfigPath = normalizePath(configPath);

        // 匹配所有路径
        if (normalizedConfigPath.equals("*") || normalizedConfigPath.equals("/*")) {
            return true;
        }

        // 前缀匹配
        if (normalizedConfigPath.endsWith("*")) {
            String prefix = normalizedConfigPath.substring(0, normalizedConfigPath.length() - 1);
            return requestPath.startsWith(prefix);
        }

        return false;
    }

    private void processRequest(GutoolFuncTabPanelDefineHttp config, HttpServletRequest request,
                                HttpServletResponse response) {
        Long funcId = config.getHttpTriggerFuncId();
        if (funcId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "未配置触发脚本ID");
            return;
        }

        // 查找对应的函数
        GutoolFunc func = GutoolFuncContainer.getFuncById(funcId);
        if (func == null) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "找不到ID为 " + funcId + " 的脚本");
            return;
        }
        // 设置异步上下文
        final AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        final String normalizePath = normalizePath(request.getRequestURI());
        // 执行脚本
        func.initRunner(panel, "",
                        message -> {
                            if (outputCallback != null) {
                                outputCallback.accept(GutoolOutputFormatter.formatHttpLog(config.getMethod(), normalizePath, message));
                            }
                        },
                        () -> {
                        })
                .asyncRun(result -> {
                    handleScriptResult(result, config, asyncContext);
                    func.resetRunner();
                });
    }

    /**
     * 处理脚本执行结果
     */
    private void handleScriptResult(Object result, GutoolFuncTabPanelDefineHttp config,
                                    AsyncContext asyncContext) {
        try {
            String resultText = formatResult(result);

            // 输出回调
            if (outputCallback != null && StrUtil.isNotBlank(resultText)) {
                outputCallback.accept(GutoolOutputFormatter.formatResultPrefix(resultText));
            }

            // 结果处理器
            if (resultHandler != null) {
                resultHandler.accept(result);
            }
            // 发送响应
            sendSuccessResponse((HttpServletResponse) asyncContext.getResponse(), config, resultText);

        } catch (Exception e) {
            log.error("处理脚本结果时发生错误", e);
            sendErrorResponse((HttpServletResponse) asyncContext.getResponse(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "处理脚本结果时发生错误: " + e.getMessage());
        } finally {
            asyncContext.complete();
        }
    }

    /**
     * 格式化结果
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "";
        }

        try {
            if (result instanceof CharSequence) {
                return result.toString();
            } else {
                return JSONUtil.toJsonPrettyStr(result);
            }
        } catch (Exception ex) {
            return GutoolExceptionHandler.formatShortException(ex);
        }
    }


    private void sendSuccessResponse(HttpServletResponse response, GutoolFuncTabPanelDefineHttp config,
                                     String resultText) {
        try {
            // 设置响应头
            String contentType = config.getContentType() != null ? config.getContentType() : "application/json";
            response.setContentType(contentType);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setStatus(HttpServletResponse.SC_OK);

            // 处理内容
            String content = resultText != null ? resultText : "";
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            // 发送响应
            try (OutputStream os = response.getOutputStream()) {
                os.write(contentBytes);
                os.flush();
            }
        } catch (IOException e) {
            handleResponseIOException(e);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setStatus(statusCode);

            // 创建标准化错误响应
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("status", statusCode);
            errorResponse.put("error", message);
            errorResponse.put("timestamp", new Date().getTime());

            String errorJson = JSONUtil.toJsonStr(errorResponse);
            byte[] errorBytes = errorJson.getBytes(StandardCharsets.UTF_8);
            response.setContentLength(errorBytes.length);
            response.getOutputStream().write(errorBytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            handleResponseIOException(e);
        }
    }

    /**
     * 处理响应IO异常
     */
    private void handleResponseIOException(IOException e) {
        if (e instanceof EofException) {
            log.debug("客户端连接已关闭，无法发送响应: {}", e.getMessage());
        } else {
            log.error("发送响应时发生错误", e);
        }
    }
}
