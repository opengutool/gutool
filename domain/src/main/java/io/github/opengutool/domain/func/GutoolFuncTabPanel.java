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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Data
public class GutoolFuncTabPanel implements Serializable {

    private Long id;

    private String name;

    private GutoolFuncTabPanelDefine define;

    private String remark;

    private String createTime;

    private String modifiedTime;


    public boolean setFuncIn(String funcIn) {
        this.getDefine().setFuncIn(funcIn);
        return true;
    }

    public boolean addButton(GutoolFuncTabPanelDefineButton defineButton) {
        this.getDefine().getButtons().add(defineButton);
        this.getDefine().getButtons().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineButton::getOrder));
        return true;
    }

    public boolean removeButton(String btnTextValue) {
        this.getDefine().getButtons().removeIf(button -> button.getText().equals(btnTextValue));
        return true;
    }

    public List<GutoolFuncTabPanelDefineButton> getButtons() {
        return define.getButtons();
    }

    public boolean getOutTextEnabled() {
        return this.getDefine().getOutTextEnabled();
    }

    public boolean sortButtons() {
        this.getDefine().getButtons().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineButton::getOrder));
        return true;
    }

    public boolean setAll(String name, String remark, boolean outTextEnabled, String type) {
        if (Objects.isNull(this.getDefine())) {
            this.setDefine(new GutoolFuncTabPanelDefine());
        }
        this.getDefine().setOutTextEnabled(outTextEnabled);
        this.getDefine().setType(type);
        this.setName(name);
        this.setRemark(remark);
        return true;
    }

    // ===== Cron 任务管理方法 =====

    /**
     * 添加 cron 任务
     *
     * @param cron cron 任务
     * @return 是否添加成功
     */
    public boolean addCrontab(GutoolFuncTabPanelDefineCron cron) {
        if (Objects.isNull(this.getDefine())) {
            this.setDefine(new GutoolFuncTabPanelDefine());
        }
        this.getDefine().getCrontab().add(cron);
        this.getDefine().getCrontab().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineCron::getOrder));
        return true;
    }

    /**
     * 移除 cron 任务
     *
     * @param cron 要移除的 cron 任务
     * @return 是否移除成功
     */
    public boolean removeCron(GutoolFuncTabPanelDefineCron cron) {
        if (Objects.nonNull(this.getDefine()) && Objects.nonNull(this.getDefine().getCrontab())) {
            return this.getDefine().getCrontab().remove(cron);
        }
        return false;
    }


    /**
     * 获取所有 cron 任务
     *
     * @return cron 任务列表
     */
    public List<GutoolFuncTabPanelDefineCron> getCrontab() {
        return this.getDefine().getCrontab();
    }

    /**
     * 排序 cron 任务
     *
     * @return 是否排序成功
     */
    public boolean sortCrontab() {
        if (Objects.nonNull(this.getDefine()) && Objects.nonNull(this.getDefine().getCrontab())) {
            this.getDefine().getCrontab().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineCron::getOrder));
            return true;
        }
        return false;
    }

    /**
     * 添加 HTTP 配置
     *
     * @param httpConfig HTTP 配置
     * @return 是否添加成功
     */
    public boolean addHttpConfig(GutoolFuncTabPanelDefineHttp httpConfig) {
        if (Objects.isNull(this.getDefine())) {
            this.setDefine(new GutoolFuncTabPanelDefine());
        }
        this.getDefine().getHttpConfigs().add(httpConfig);
        this.getDefine().getHttpConfigs().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineHttp::getOrder));
        return true;
    }

    /**
     * 移除 HTTP 配置
     *
     * @param httpConfig 要移除的 HTTP 配置
     * @return 是否移除成功
     */
    public boolean removeHttpConfig(GutoolFuncTabPanelDefineHttp httpConfig) {
        if (Objects.nonNull(this.getDefine()) && Objects.nonNull(this.getDefine().getHttpConfigs())) {
            return this.getDefine().getHttpConfigs().remove(httpConfig);
        }
        return false;
    }

    /**
     * 获取所有 HTTP 配置
     *
     * @return HTTP 配置列表
     */
    public List<GutoolFuncTabPanelDefineHttp> getHttpConfigs() {
        return this.getDefine().getHttpConfigs();
    }


    /**
     * 排序 HTTP 配置
     *
     * @return 是否排序成功
     */
    public boolean sortHttpConfigs() {
        if (Objects.nonNull(this.getDefine()) && Objects.nonNull(this.getDefine().getHttpConfigs())) {
            this.getDefine().getHttpConfigs().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineHttp::getOrder));
            return true;
        }
        return false;
    }

    public boolean setHttpPort(Integer httpPort) {
        this.getDefine().setPort(httpPort);
        return true;
    }

    public boolean setAutoEnabled(boolean httpStart) {
        this.getDefine().setAutoEnabled(httpStart);
        return true;
    }

    public boolean setThreadPoolSize(Integer threadPoolSize) {
        this.getDefine().setThreadPoolSize(threadPoolSize);
        return true;
    }
}
