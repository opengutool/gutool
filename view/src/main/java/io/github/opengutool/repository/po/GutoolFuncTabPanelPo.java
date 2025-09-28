package io.github.opengutool.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefine;

import java.io.Serializable;

@TableName("gutool_func_tab_panel")
public class GutoolFuncTabPanelPo implements Serializable {
    private Long id;

    private String name;

    /**
     * {@link GutoolFuncTabPanelDefine}
     */
    private String define;

    private String remark;

    private String createTime;

    private String modifiedTime;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefine() {
        return define;
    }

    public void setDefine(String define) {
        this.define = define;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
