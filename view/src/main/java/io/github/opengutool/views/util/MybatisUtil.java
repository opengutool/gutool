package io.github.opengutool.views.util;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import io.github.opengutool.repository.GutoolDbRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
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
    private static SqlSession sqlSession = null;

    private MybatisUtil() {
    }

    public static SqlSession getSqlSession() {
        if (sqlSession == null) {
            DataSource dataSource = GutoolDbRepository.getDataSource();
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("Production", transactionFactory, dataSource);
            MybatisConfiguration configuration = new MybatisConfiguration(environment);
            configuration.addMappers("io.github.opengutool.repository.mapper");
            configuration.setLogImpl(Slf4jImpl.class);
            sqlSession = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        }
        return sqlSession;
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


    public static void setSqlSession(SqlSession sqlSession) {
        MybatisUtil.sqlSession = sqlSession;
    }
}
