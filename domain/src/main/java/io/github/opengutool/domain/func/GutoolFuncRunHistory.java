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

import io.github.opengutool.domain.script.GutoolScriptEvent;
import lombok.Data;

import java.util.Date;
import java.util.Objects;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Data
public class GutoolFuncRunHistory {

    private Long id;

    private Long funcId;

    private Long tabPanelId;

    private String funcMirror;

    private String funcIn;

    private String funcOut;

    private String status;

    private Integer costTime;

    private Date createTime;

    private Date modifiedTime;

    private GutoolScriptEvent event;

    public GutoolFuncRunHistory(GutoolFunc func, GutoolFuncTabPanel funcTabPanel, String funcIn) {
        this.setFuncId(func.getId());
        if (Objects.nonNull(funcTabPanel)) {
            this.setTabPanelId(funcTabPanel.getId());
        }
        this.setFuncMirror(func.getContent());
        this.setFuncIn(funcIn);
        this.setFuncOut("");
        this.setStatus("");
        this.setCostTime(0);
        // 记录时间
        this.setCreateTime(new Date());
        this.setModifiedTime(this.getCreateTime());

        event = new GutoolScriptEvent();
        event.setType(funcTabPanel.getDefine().getType());
        event.setTimestamp(this.getCreateTime().getTime());
        event.setFuncId(this.getFuncId());
        event.setTabPanelId(this.getTabPanelId());
    }

    public void setId(Long id) {
        this.id = id;
        event.setHistoryId(id);
    }

    /**
     * 更新输出和状态
     */
    public void update(String funcOut, String status) {
        final Date refreshTime = new Date();
        this.setFuncOut(funcOut);
        this.setStatus(status);
        // 记录时间
        this.setCostTime((int) (refreshTime.getTime() - this.getCreateTime().getTime()));
        this.setModifiedTime(refreshTime);
    }
}
