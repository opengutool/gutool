package io.github.opengutool.repository.convert;

import cn.hutool.json.JSONUtil;
import io.github.opengutool.domain.func.GutoolFunc;
import io.github.opengutool.domain.func.GutoolFuncRunHistory;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefine;
import io.github.opengutool.repository.GutoolDbRepository;
import io.github.opengutool.repository.po.GutoolFuncPo;
import io.github.opengutool.repository.po.GutoolFuncRunHistoryPo;
import io.github.opengutool.repository.po.GutoolFuncTabPanelPo;

import java.io.Serializable;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolPoConvert implements Serializable {
    public static GutoolFunc convertFunc(GutoolFuncPo funcPo) {
        GutoolFunc func = new GutoolFunc();
        func.setName(funcPo.getName());
        func.setId(funcPo.getId());
        func.setDefine(funcPo.getDefine());
        func.setContent(funcPo.getContent());
        func.setRemark(funcPo.getRemark());
        func.setCreateTime(funcPo.getCreateTime());
        func.setModifiedTime(funcPo.getModifiedTime());
        return func;
    }

    public static GutoolFuncPo convertFuncPo(GutoolFunc func) {
        GutoolFuncPo funcPo = new GutoolFuncPo();
        funcPo.setName(func.getName());
        funcPo.setId(func.getId());
        funcPo.setDefine(func.getDefine());
        funcPo.setContent(func.getContent());
        funcPo.setRemark(func.getRemark());
        funcPo.setCreateTime(func.getCreateTime());
        funcPo.setModifiedTime(func.getModifiedTime());
        return funcPo;
    }

    public static GutoolFuncRunHistoryPo convertFuncRunHistoryPo(GutoolFuncRunHistory history) {
        GutoolFuncRunHistoryPo historyPo = new GutoolFuncRunHistoryPo();
        historyPo.setId(history.getId());
        historyPo.setFuncId(history.getFuncId());
        historyPo.setTabPanelId(history.getTabPanelId());
        historyPo.setFuncMirror(history.getFuncMirror());
        historyPo.setFuncIn(history.getFuncIn());
        historyPo.setFuncOut(history.getFuncOut());
        historyPo.setStatus(history.getStatus());
        historyPo.setCostTime(history.getCostTime());
        historyPo.setCreateTime(GutoolDbRepository.dateFormatForSqlite(history.getCreateTime()));
        historyPo.setModifiedTime(GutoolDbRepository.dateFormatForSqlite(history.getModifiedTime()));
        return historyPo;
    }

    public static GutoolFuncTabPanel convertFuncTabPanel(GutoolFuncTabPanelPo funcTabPanelPo) {
        GutoolFuncTabPanel funcTabPanel = new GutoolFuncTabPanel();
        funcTabPanel.setId(funcTabPanelPo.getId());
        funcTabPanel.setName(funcTabPanelPo.getName());
        funcTabPanel.setDefine(JSONUtil.toBean(funcTabPanelPo.getDefine(), GutoolFuncTabPanelDefine.class));
        funcTabPanel.setRemark(funcTabPanelPo.getRemark());
        funcTabPanel.setCreateTime(funcTabPanelPo.getCreateTime());
        funcTabPanel.setModifiedTime(funcTabPanelPo.getModifiedTime());
        return funcTabPanel;
    }

    public static GutoolFuncTabPanelPo convertFuncTabPanelPo(GutoolFuncTabPanel funcTabPanel) {
        GutoolFuncTabPanelPo funcTabPanelPo = new GutoolFuncTabPanelPo();
        funcTabPanelPo.setId(funcTabPanel.getId());
        funcTabPanelPo.setName(funcTabPanel.getName());
        funcTabPanelPo.setDefine(JSONUtil.toJsonStr(funcTabPanel.getDefine()));
        funcTabPanelPo.setRemark(funcTabPanel.getRemark());
        funcTabPanelPo.setCreateTime(funcTabPanel.getCreateTime());
        funcTabPanelPo.setModifiedTime(funcTabPanel.getModifiedTime());
        return funcTabPanelPo;
    }
}
