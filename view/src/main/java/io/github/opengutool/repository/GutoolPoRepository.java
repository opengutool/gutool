package io.github.opengutool.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import io.github.opengutool.common.ddd.GutoolDDDFactory;
import io.github.opengutool.common.ddd.GutoolDDDMethodAroundType;
import io.github.opengutool.domain.func.GutoolFunc;
import io.github.opengutool.domain.func.GutoolFuncContainer;
import io.github.opengutool.domain.func.GutoolFuncRunHistory;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.repository.convert.GutoolPoConvert;
import io.github.opengutool.repository.mapper.GutoolFuncMapper;
import io.github.opengutool.repository.mapper.GutoolFuncRunHistoryMapper;
import io.github.opengutool.repository.mapper.GutoolFuncTabPanelMapper;
import io.github.opengutool.repository.po.GutoolFuncPo;
import io.github.opengutool.repository.po.GutoolFuncRunHistoryPo;
import io.github.opengutool.views.util.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.Objects;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolPoRepository {
    private static SqlSession sqlSession = null;

    public static GutoolFuncMapper funcMapper = null;
    public static GutoolFuncRunHistoryMapper funcRunHistoryMapper = null;
    public static GutoolFuncTabPanelMapper funcTabPanelMapper = null;

    public static void init() {
        GenericTypeUtils.setGenericTypeResolver(GutoolGenericTypeResolver.INSTANCE);
        // 初始化
        sqlSession = MybatisUtil.getSqlSession();
        funcMapper = sqlSession.getMapper(GutoolFuncMapper.class);
        funcRunHistoryMapper = sqlSession.getMapper(GutoolFuncRunHistoryMapper.class);
        funcTabPanelMapper = sqlSession.getMapper(GutoolFuncTabPanelMapper.class);
        // 监听
        initListen();
    }


    private static void initListen() {
        LambdaQueryWrapper<GutoolFuncPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderBy(true, true, GutoolFuncPo::getId);
        GutoolPoRepository.funcMapper.selectList(queryWrapper)
                .forEach(funcPo ->
                        GutoolFuncContainer.putFunc(
                                GutoolDDDFactory.create(
                                        GutoolPoConvert.convertFunc(funcPo))));
        GutoolDDDFactory.listen(
                GutoolFunc.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> {
                    if (Boolean.TRUE.equals(event.getProceedResult())) {
                        String now = GutoolDbRepository.nowDateForSqlite();
                        GutoolFunc func = event.getSource();
                        if (Objects.isNull(func.getId())) {
                            // 先更新 source
                            func.setId(IdWorker.getId());
                            func.setCreateTime(now);
                            func.setModifiedTime(now);
                            // 转换
                            GutoolFuncPo insert = GutoolPoConvert.convertFuncPo(func);
                            funcMapper.insert(insert);

                            GutoolFuncContainer.putFunc(
                                    GutoolDDDFactory.create(
                                            GutoolPoConvert.convertFunc(insert)));
                        } else {
                            // 先更新 source
                            func.setModifiedTime(now);
                            // 转换
                            GutoolFuncPo update = GutoolPoConvert.convertFuncPo(func);
                            funcMapper.updateById(update);
                        }
                    }
                },
                GutoolFunc::setName,
                GutoolFunc::updateScript,
                GutoolFunc::formatUpdateScript);

        // init 之后把数据入库
        GutoolDDDFactory.listen(
                GutoolFunc.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> {
                    GutoolFuncRunHistory history = event.getSource().getFuncRunHistory();
                    if (Objects.nonNull(history) && Objects.isNull(history.getId())) {
                        // 先更新 source
                        history.setId(IdWorker.getId());
                        // 转换
                        GutoolFuncRunHistoryPo insert = GutoolPoConvert.convertFuncRunHistoryPo(history);
                        funcRunHistoryMapper.insert(insert);
                    }
                },
                GutoolFunc::initRunner);
        // reset 之前把数据入库
        GutoolDDDFactory.listen(
                GutoolFunc.class,
                GutoolDDDMethodAroundType.BEFORE,
                (event) -> {
                    GutoolFuncRunHistory history = event.getSource().getFuncRunHistory();
                    if (Objects.nonNull(history) && Objects.nonNull(history.getId())) {
                        // 转换
                        funcRunHistoryMapper.updateById(GutoolPoConvert.convertFuncRunHistoryPo(history));
                    }
                },
                GutoolFunc::resetRunner);

        GutoolDDDFactory.listen(
                GutoolFuncTabPanel.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> insertOrUpdateFuncTabPanel(event.getSource()),
                GutoolFuncTabPanel::setName,
                GutoolFuncTabPanel::removeButton);
        GutoolDDDFactory.listen(
                GutoolFuncTabPanel.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> insertOrUpdateFuncTabPanel(event.getSource()),
                GutoolFuncTabPanel::addButton);
        GutoolDDDFactory.listen(
                GutoolFuncTabPanel.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> insertOrUpdateFuncTabPanel(event.getSource()),
                GutoolFuncTabPanel::sortButtons);
        GutoolDDDFactory.listen(
                GutoolFuncTabPanel.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> insertOrUpdateFuncTabPanel(event.getSource()),
                GutoolFuncTabPanel::setAll);
        GutoolDDDFactory.listen(
                GutoolFuncTabPanel.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> insertOrUpdateFuncTabPanel(event.getSource()),
                GutoolFuncTabPanel::addCrontab, GutoolFuncTabPanel::removeCron);
        GutoolDDDFactory.listen(
                GutoolFuncTabPanel.class,
                GutoolDDDMethodAroundType.AFTER,
                (event) -> insertOrUpdateFuncTabPanel(event.getSource()),
                GutoolFuncTabPanel::sortCrontab);
    }

    // funcTabPanel
    public static void insertOrUpdateFuncTabPanel(GutoolFuncTabPanel insertOrUpdate) {
        String now = GutoolDbRepository.nowDateForSqlite();
        if (Objects.isNull(insertOrUpdate.getId())) {
            insertOrUpdate.setId(IdWorker.getId());
            insertOrUpdate.setCreateTime(now);
            insertOrUpdate.setModifiedTime(now);
            funcTabPanelMapper.insert(GutoolPoConvert.convertFuncTabPanelPo(insertOrUpdate));
        } else {
            insertOrUpdate.setModifiedTime(now);
            funcTabPanelMapper.updateById(GutoolPoConvert.convertFuncTabPanelPo(insertOrUpdate));
        }
    }

    // funcRunHistory
    public static void deleteFuncRunHistoryById(Long historyId) {
        funcRunHistoryMapper.deleteById(historyId);
    }

    public static void deleteAllFuncRunHistoryByFuncId(Long funcId) {
        LambdaQueryWrapper<GutoolFuncRunHistoryPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GutoolFuncRunHistoryPo::getFuncId, funcId);
        funcRunHistoryMapper.delete(queryWrapper);
    }

    public static void deleteAllFuncRunHistoryByTabPanelId(Long tabPanelId) {
        LambdaQueryWrapper<GutoolFuncRunHistoryPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GutoolFuncRunHistoryPo::getTabPanelId, tabPanelId);
        funcRunHistoryMapper.delete(queryWrapper);
    }
}
