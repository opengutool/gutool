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

import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineHttp;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/20
 */
@Slf4j
public class GutoolHttpServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int MIN_THREADS = 8;
    private static final int MAX_THREADS = 20;
    private static final int IDLE_TIMEOUT = 30000;

    private Server server;
    private ServerConnector connector;
    private final GutoolFuncTabPanel panel;
    private final Consumer<String> outputCallback;
    private final Consumer<Object> resultHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private GutoolHttpRequestHandler requestHandler;

    public GutoolHttpServer(GutoolFuncTabPanel panel, Consumer<String> outputCallback, Consumer<Object> resultHandler) {
        this.panel = panel;
        this.outputCallback = outputCallback;
        this.resultHandler = resultHandler;
    }


    public synchronized void start() throws Exception {
        if (running.get()) {
            log.warn("HTTP服务器已经在运行中");
            return;
        }

        try {
            validateConfiguration();
            createServer();
            configureHandlers();
            startServer();
            running.set(true);
            log.info("HTTP服务器启动成功，端口: {}", getPort());
        } catch (Exception e) {
            log.error("启动HTTP服务器失败", e);
            cleanup();
            throw e;
        }
    }

    /**
     * 重启HTTP服务器
     */
    public synchronized void restart() throws Exception {
        log.info("重启HTTP服务器");
        stop();
        start();
    }

    /**
     * 停止HTTP服务器，确保资源正确释放
     */
    public synchronized void stop() throws Exception {
        if (!running.get()) {
            log.debug("HTTP服务器未运行");
            return;
        }

        try {
            log.info("停止HTTP服务器");
            if (server != null) {
                server.stop();
                server.join();
            }
            running.set(false);
            log.info("HTTP服务器已停止");
        } catch (Exception e) {
            log.error("停止HTTP服务器时发生错误", e);
            throw e;
        } finally {
            cleanup();
        }
    }

    /**
     * 验证配置
     */
    private void validateConfiguration() {
        if (panel == null) {
            throw new IllegalArgumentException("面板配置不能为空");
        }

        if (panel.getHttpConfigs() == null || panel.getHttpConfigs().isEmpty()) {
            log.warn("没有配置HTTP端点");
        }

        int port = panel.getDefine().getPort();
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("无效的端口号: " + port);
        }
    }

    /**
     * 创建Jetty服务器实例
     */
    private void createServer() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(MIN_THREADS);
        threadPool.setMaxThreads(MAX_THREADS);
        threadPool.setIdleTimeout(IDLE_TIMEOUT);
        threadPool.setName("gutool-http-server-[" + panel.getName() + "]");

        server = new Server(threadPool);

        // 配置HTTP连接器，移除默认的Server头
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);

        connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setPort(panel.getDefine().getPort());
        connector.setIdleTimeout(IDLE_TIMEOUT);
        connector.setHost("0.0.0.0");

        server.setConnectors(new Connector[]{connector});
    }

    /**
     * 配置处理器
     */
    private void configureHandlers() {
        // 创建配置列表的副本，避免并发修改问题
        final List<GutoolFuncTabPanelDefineHttp> httpConfigs = new ArrayList<>(panel.getHttpConfigs());

        // 创建请求处理器
        requestHandler = new GutoolHttpRequestHandler(panel, httpConfigs, outputCallback, resultHandler);

        // 配置Servlet上下文
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(requestHandler), "/*");
        server.setHandler(context);
    }

    /**
     * 启动服务器
     */
    private void startServer() throws Exception {
        server.start();
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        if (server != null) {
            try {
                if (!server.isStopped()) {
                    server.stop();
                }
                server.destroy();
            } catch (Exception e) {
                log.warn("清理Jetty服务器资源时发生错误", e);
            } finally {
                server = null;
                requestHandler = null;
                connector = null;
            }
        }
    }

    /**
     * 获取端口号
     */
    public int getPort() {
        return Objects.isNull(connector) ? 0 : connector.getLocalPort();
    }

    /**
     * 检查服务器是否运行中
     */
    public boolean isRunning() {
        return running.get() && server != null && server.isRunning();
    }

    /**
     * 获取服务器状态信息
     */
    public String getServerStatus() {
        if (!running.get()) {
            return "已停止";
        }
        if (server == null) {
            return "初始化中";
        }
        if (server.isStarting()) {
            return "启动中";
        }
        if (server.isRunning()) {
            return "运行中";
        }
        if (server.isStopping()) {
            return "停止中";
        }
        if (server.isFailed()) {
            return "失败";
        }
        return "未知状态";
    }
}