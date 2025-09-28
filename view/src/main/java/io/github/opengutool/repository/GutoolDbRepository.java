package io.github.opengutool.repository;

import io.github.opengutool.GutoolApp;
import io.github.opengutool.views.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.util.Date;

/**
 * <pre>
 * Sqlite相关工具
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolDbRepository {

    private static volatile File dbFile = new File(SystemUtil.CONFIG_HOME + File.separator + "gutool.db");

    public static String getDbFileParentPath() {
        return dbFile.getParent();
    }

    public static DataSource getDataSource() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(dbUrl());
        return dataSource;
    }

    public static synchronized String dbUrl() {
        initDbDirectory();
        if (StringUtils.isNotBlank(GutoolApp.config.getDbFilePath())
                && !GutoolApp.config.getDbFilePath().equalsIgnoreCase(GutoolDbRepository.getDbFileParentPath())) {
            dbFile = new File(GutoolApp.config.getDbFilePath() + File.separator + "gutool.db");
        }
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    /**
     * 初始化数据库文件
     */
    public static void initDbDirectory() {
        File configHomeDir = new File(SystemUtil.CONFIG_HOME);
        if (!configHomeDir.exists()) {
            configHomeDir.mkdirs();
        }
    }

    public static String nowDateForSqlite() {
        return dateFormatForSqlite(new Date());
    }


    public static String dateFormatForSqlite(Date date) {
        return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
    }

}
