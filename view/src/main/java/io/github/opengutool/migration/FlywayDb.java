package io.github.opengutool.migration;

import io.github.opengutool.repository.GutoolDbRepository;
import org.flywaydb.core.Flyway;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class FlywayDb {
    public static void startMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(GutoolDbRepository.getDataSource())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }
}
