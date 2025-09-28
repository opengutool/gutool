package io.github.opengutool.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.opengutool.common.ddd.GutoolDDDFactory;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.repository.convert.GutoolPoConvert;
import io.github.opengutool.repository.po.GutoolFuncPo;
import io.github.opengutool.repository.po.GutoolFuncRunHistoryPo;
import io.github.opengutool.repository.po.GutoolFuncTabPanelPo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolPoQueryRepository {

    public static List<Object[]> selectFuncAllDataObjectList() {
        LambdaQueryWrapper<GutoolFuncPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(GutoolFuncPo::getId, GutoolFuncPo::getName);
        queryWrapper.orderBy(true, true, GutoolFuncPo::getId);
        List<GutoolFuncPo> funcList = GutoolPoRepository.funcMapper.selectList(queryWrapper);
        return funcList.stream()
                .map(func -> new Object[]{func.getId(), func.getName()}).collect(Collectors.toList());
    }

    public static GutoolFuncPo selectFuncById(Long id) {
        return GutoolPoRepository.funcMapper.selectById(id);
    }

    public static GutoolFuncPo selectFuncByName(String newName) {
        LambdaQueryWrapper<GutoolFuncPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GutoolFuncPo::getName, newName);
        queryWrapper.last("limit 1");
        return GutoolPoRepository.funcMapper.selectOne(queryWrapper);
    }

    public static List<GutoolFuncTabPanel> selectFuncTabPanelAll() {
        LambdaQueryWrapper<GutoolFuncTabPanelPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderBy(true, true, GutoolFuncTabPanelPo::getId);
        return GutoolPoRepository.funcTabPanelMapper.selectList(queryWrapper)
                .stream()
                .map(GutoolPoConvert::convertFuncTabPanel)
                .map(GutoolDDDFactory::create)
                .collect(Collectors.toList());
    }

    // funcRunHistory
    public static List<GutoolFuncRunHistoryPo> selectFuncRunHistoryListByFuncId(Long funcId) {
        LambdaQueryWrapper<GutoolFuncRunHistoryPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GutoolFuncRunHistoryPo::getFuncId, funcId);
        queryWrapper.orderBy(true, true, GutoolFuncRunHistoryPo::getId);
        return GutoolPoRepository.funcRunHistoryMapper.selectList(queryWrapper);
    }

    public static List<Object[]> selectFuncRunHistoryDataObjectListByFuncId(Long funcId) {
        return selectFuncRunHistoryListByFuncId(funcId).stream()
                .map(history -> new Object[]{
                        history.getId(),
                        history.getFuncMirror(),
                        history.getFuncIn(),
                        history.getFuncOut(),
                        history.getCostTime(),
                        history.getStatus(),
                        history.getCreateTime(),
                }).collect(Collectors.toList());
    }


    public static List<GutoolFuncRunHistoryPo> selectFuncRunHistoryListByTabPanelId(Long tabPanelId) {
        LambdaQueryWrapper<GutoolFuncRunHistoryPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GutoolFuncRunHistoryPo::getTabPanelId, tabPanelId);
        queryWrapper.orderBy(true, true, GutoolFuncRunHistoryPo::getId);
        return GutoolPoRepository.funcRunHistoryMapper.selectList(queryWrapper);
    }
}
