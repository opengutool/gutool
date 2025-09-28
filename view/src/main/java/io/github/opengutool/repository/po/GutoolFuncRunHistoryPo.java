package io.github.opengutool.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("gutool_func_run_history")
public class GutoolFuncRunHistoryPo implements Serializable {

    private Long id;

    private Long funcId;

    private Long tabPanelId;

    private String funcMirror;

    private String funcIn;

    private String funcOut;

    private String status;

    private Integer costTime;

    private String createTime;

    private String modifiedTime;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFuncId() {
        return funcId;
    }

    public void setFuncId(Long funcId) {
        this.funcId = funcId;
    }

    public Long getTabPanelId() {
        return tabPanelId;
    }

    public void setTabPanelId(Long tabPanelId) {
        this.tabPanelId = tabPanelId;
    }

    public String getFuncMirror() {
        return funcMirror;
    }

    public void setFuncMirror(String funcMirror) {
        this.funcMirror = funcMirror;
    }

    public String getFuncIn() {
        return funcIn;
    }

    public void setFuncIn(String funcIn) {
        this.funcIn = funcIn;
    }

    public String getFuncOut() {
        return funcOut;
    }

    public void setFuncOut(String funcOut) {
        this.funcOut = funcOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCostTime() {
        return costTime;
    }

    public void setCostTime(Integer costTime) {
        this.costTime = costTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
}
