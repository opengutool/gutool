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
 * HTTP服务配置
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/20
 */
@Data
public class GutoolFuncTabPanelDefineHttp implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否启用HTTP服务
     */
    private Boolean enabled = false;

    /**
     * 服务器状态
     */
    private String status = "stopped"; // stopped, running, error

    /**
     * 绑定的脚本ID
     */
    private Long httpTriggerFuncId;

    /**
     * HTTP方法
     */
    private String method = "GET"; // GET, POST, PUT, DELETE

    /**
     * 路由路径
     */
    private String path = "/";

    /**
     * 响应内容类型
     */
    private String contentType = "application/json";

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer order = 0;

    /**
     * 最后访问时间
     */
    private Date lastAccessTime;
}