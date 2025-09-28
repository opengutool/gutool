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

    List<GutoolFuncRunHistory> funcRunHistoryList;


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

    public boolean isOutTextEnabled() {
        return this.getDefine().isOutTextEnabled();
    }

    public boolean sortButtons() {
        this.getDefine().getButtons().sort(Comparator.comparingInt(GutoolFuncTabPanelDefineButton::getOrder));
        return true;
    }

    public boolean setAll(String name, String remark, boolean outTextEnabled) {
        if (Objects.isNull(this.getDefine())) {
            this.setDefine(new GutoolFuncTabPanelDefine());
        }
        this.getDefine().setOutTextEnabled(outTextEnabled);
        this.setName(name);
        this.setRemark(remark);
        return true;
    }
}
