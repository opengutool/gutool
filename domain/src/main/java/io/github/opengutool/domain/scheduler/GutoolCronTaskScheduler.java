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
package io.github.opengutool.domain.scheduler;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.github.opengutool.domain.func.GutoolFunc;
import io.github.opengutool.domain.func.GutoolFuncContainer;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineCron;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 定时任务调度器
 * 每个 ScriptRunnerForm 实例应该创建自己的调度器实例来管理定时任务
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/10
 */
@Slf4j
public class GutoolCronTaskScheduler {

    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks;
    private final GutoolFuncTabPanel panel;
    private final Consumer<String> outputCallback;
    private final Consumer<Object> resultHandler;
    private final CronDefinition cronDefinition;
    private final CronParser cronParser;

    public GutoolCronTaskScheduler(GutoolFuncTabPanel panel, Consumer<String> outputCallback, Consumer<Object> resultHandler) {
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.panel = panel;
        this.outputCallback = outputCallback;
        this.resultHandler = resultHandler;
        this.cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        this.cronParser = new CronParser(cronDefinition);
    }

    /**
     * 启动定时任务
     */
    public void startTasks() {
        if (panel == null || panel.getCrontab() == null) {
            return;
        }

        List<GutoolFuncTabPanelDefineCron> cronTasks = panel.getCrontab();
        for (GutoolFuncTabPanelDefineCron cron : cronTasks) {
            if (cron.getEnabled()) {
                scheduleTask(cron);
            }
        }

        log.info("启动面板 {} 的 {} 个定时任务", panel.getName(), cronTasks.size());
    }

    /**
     * 停止定时任务
     */
    public void stopTasks() {
        if (panel == null || panel.getCrontab() == null) {
            return;
        }

        for (GutoolFuncTabPanelDefineCron cron : panel.getCrontab()) {
            stopTask(cron);
        }

        log.info("停止面板 {} 的所有定时任务", panel.getName());
    }

    /**
     * 调度单个定时任务
     */
    public void scheduleTask(GutoolFuncTabPanelDefineCron cron) {
        if (cron == null || !cron.getEnabled() || cron.getCronTriggerFuncId() == null) {
            return;
        }

        try {
            // 解析 Cron 表达式
            Cron parsedCron = cronParser.parse(cron.getCronExpression());
            ExecutionTime executionTime = ExecutionTime.forCron(parsedCron);

            // 计算下次执行时间
            ZonedDateTime now = ZonedDateTime.now();
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);

            if (nextExecution.isPresent()) {
                ZonedDateTime nextTime = nextExecution.get();
                long delay = java.time.Duration.between(now, nextTime).toMillis();

                // 调度任务
                ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
                    executeCronTask(cron);
                    // 递归调度下次执行
                    scheduleTask(cron);
                }, delay, TimeUnit.MILLISECONDS);

                // 存储调度任务，用于可能的取消操作
                scheduledTasks.put(cron.getCronTriggerFuncId(), scheduledTask);

                // 更新任务的下次执行时间
                cron.setNextExecutionTime(java.sql.Timestamp.valueOf(nextTime.toLocalDateTime()));

                log.info("调度定时任务 [{}] 下次执行时间: {}", cron.getDescription(), nextTime);
            } else {
                log.warn("定时任务 [{}] 无有效的下次执行时间", cron.getDescription());
            }
        } catch (Exception e) {
            log.error("调度定时任务 [{}] 失败: {}", cron.getDescription(), e.getMessage(), e);
        }
    }

    /**
     * 停止单个定时任务
     */
    public void stopTask(GutoolFuncTabPanelDefineCron cron) {
        if (cron == null || cron.getCronTriggerFuncId() == null) {
            return;
        }

        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(cron.getCronTriggerFuncId());
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            log.info("停止定时任务 [{}]", cron.getDescription());
        }
    }

    /**
     * 执行定时任务
     */
    private void executeCronTask(GutoolFuncTabPanelDefineCron cron) {
        if (!cron.getEnabled()) {
            return;
        }

        try {
            // 获取要执行的脚本
            GutoolFunc func = GutoolFuncContainer.getFuncById(cron.getCronTriggerFuncId());
            if (func == null) {
                log.error("定时任务 [{}] 找不到关联的脚本 ID: {}", cron.getDescription(), cron.getCronTriggerFuncId());
                return;
            }

            if (func.getContent() == null || func.getContent().trim().isEmpty()) {
                log.error("定时任务 [{}] 关联的脚本内容为空", cron.getDescription());
                return;
            }

            // 更新执行时间
            cron.setLastExecutionTime(new java.sql.Timestamp(System.currentTimeMillis()));

            log.info("开始执行定时任务 [{}]", cron.getDescription());

            // 添加执行信息
            String executionInfo = String.format("执行定时任务: %s [%s]\n",
                    cron.getDescription(), cron.getCronExpression());
            if (this.outputCallback != null) {
                this.outputCallback.accept(executionInfo);
            }

            // 异步执行脚本
            func.initRunner(panel, "",
                    this.outputCallback,
                    () -> {
                        // 执行完成后的回调
                    }
            ).asyncRun(result -> {
                String resultText = "";
                try {
                    if (result instanceof CharSequence) {
                        resultText = result.toString();
                    } else {
                        resultText = JSONUtil.toJsonPrettyStr(result);
                    }
                } catch (Exception ex) {
                    resultText = ExceptionUtil.stacktraceToString(ex, 500);
                }

                if (this.outputCallback != null) {
                    this.outputCallback.accept("result:\n");
                    this.outputCallback.accept(resultText);
                    this.outputCallback.accept("\n");
                }
                if (Objects.nonNull(this.resultHandler)) {
                    this.resultHandler.accept(result);
                }
                func.resetRunner();
            });
        } catch (Exception e) {
            log.error("执行定时任务 [{}] 时发生错误: {}", cron.getDescription(), e.getMessage(), e);
        }
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("定时任务调度器已关闭");
    }
}