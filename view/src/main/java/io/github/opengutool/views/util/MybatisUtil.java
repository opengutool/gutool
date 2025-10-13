package io.github.opengutool.views.util;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import io.github.opengutool.repository.GutoolDbRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <pre>
 * Mybatis工具
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Slf4j
public class MybatisUtil {
    private static volatile SqlSessionFactory sqlSessionFactory = null;

    private static final ThreadLocal<SqlSession> THREAD_LOCAL_SESSION = new ThreadLocal<>();

    private MybatisUtil() {
    }

    /**
     * 初始化SqlSessionFactory（单例）
     */
    private static void initSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            synchronized (MybatisUtil.class) {
                if (sqlSessionFactory == null) {
                    DataSource dataSource = GutoolDbRepository.getDataSource();
                    TransactionFactory transactionFactory = new JdbcTransactionFactory();
                    Environment environment = new Environment("Production", transactionFactory, dataSource);
                    MybatisConfiguration configuration = new MybatisConfiguration(environment);
                    configuration.addMappers("io.github.opengutool.repository.mapper");
                    configuration.setLogImpl(Slf4jImpl.class);
                    try {
                        registryMapperXml(configuration);
                    } catch (IOException e) {
                        log.error("注册Mapper XML失败", e);
                    }
                    sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
                }
            }
        }
    }

    /**
     * 获取SqlSessionFactory，用于外部创建自己的SqlSession
     */
    public static SqlSessionFactory getSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            initSqlSessionFactory();
        }
        return sqlSessionFactory;
    }

    /**
     * 在新事务中执行操作（创建独立的SqlSession）
     */
    public static <T, R> R executeWithMapper(Class<T> mapperClass, MapperCallback<T, R> callback) {
        if (sqlSessionFactory == null) {
            initSqlSessionFactory();
        }

        SqlSession session = sqlSessionFactory.openSession(true);
        try {
            T mapper = session.getMapper(mapperClass);
            return callback.doWithMapper(mapper);
        } catch (Exception e) {
            log.error("数据库操作失败", e);
            throw e;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    log.error("关闭SqlSession失败", e);
                }
            }
        }
    }

    /**
     * 关闭当前线程的SqlSession（关键方法）
     */
    public static void closeCurrentSession() {
        SqlSession session = THREAD_LOCAL_SESSION.get();
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("关闭SqlSession失败", e);
            } finally {
                THREAD_LOCAL_SESSION.remove();
            }
        }
    }

    /**
     * 清理MyBatis资源（应用关闭时调用）
     */
    public static void shutdown() {
        try {
            closeCurrentSession();
        } finally {
            sqlSessionFactory = null;
            log.info("MyBatis资源清理完成");
        }
    }

    /**
     * 重置MyBatis配置（用于配置变更后重新初始化）
     */
    public static void reset() {
        closeCurrentSession();
        sqlSessionFactory = null;
    }

    private static void registryMapperXml(MybatisConfiguration configuration) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = loader.getResources("mapper");

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if ("file".equals(url.getProtocol())) {
                File dir = new File(url.getPath());
                if (dir.isDirectory()) {
                    for (File file : Objects.requireNonNull(dir.listFiles())) {
                        if (file.getName().endsWith(".xml")) {
                            try (InputStream input = new FileInputStream(file)) {
                                new XMLMapperBuilder(input, configuration, file.toString(), configuration.getSqlFragments()).parse();
                            }
                        }
                    }
                }
            } else if ("jar".equals(url.getProtocol())) {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jarFile = conn.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("mapper") && entry.getName().endsWith(".xml")) {
                        try (InputStream input = jarFile.getInputStream(entry)) {
                            new XMLMapperBuilder(input, configuration, entry.getName(), configuration.getSqlFragments()).parse();
                        }
                    }
                }
            }
        }
    }

    /**
     * Mapper操作回调接口
     */
    public interface MapperCallback<T, R> {
        R doWithMapper(T mapper);
    }
}
