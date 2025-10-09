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
package io.github.opengutool.domain.func;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务配置
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/30
 */
@Data
public class GutoolFuncTabPanelDefineCron implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 描述
     */
    private String description;

    /**
     * 绑定的脚本ID
     */
    private Long cronTriggerFuncId;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 下次执行时间
     */
    private Date nextExecutionTime;

    /**
     * 上次执行时间
     */
    private Date lastExecutionTime;

    /**
     * 排序
     */
    private Integer order = 0;
}
