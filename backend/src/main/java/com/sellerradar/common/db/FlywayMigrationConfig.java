package com.sellerradar.common.db;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayMigrationConfig {
	private static final String FLYWAY_MIGRATION_BEAN = "flywayMigration";
	private static final String ENTITY_MANAGER_FACTORY_BEAN = "entityManagerFactory";

	@Bean
	Flyway flyway(
			DataSource dataSource,
			@Value("${spring.flyway.locations:classpath:db/migration}") String locations,
			@Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
			@Value("${spring.flyway.baseline-version:0}") String baselineVersion
	) {
		return Flyway.configure()
				.dataSource(dataSource)
				.locations(splitLocations(locations))
				.baselineOnMigrate(baselineOnMigrate)
				.baselineVersion(MigrationVersion.fromVersion(baselineVersion))
				.load();
	}

	@Bean(name = FLYWAY_MIGRATION_BEAN)
	InitializingBean flywayMigration(Flyway flyway) {
		return flyway::migrate;
	}

	@Bean
	static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlywayMigration() {
		return new FlywayDependsOnPostProcessor();
	}

	private static String[] splitLocations(String locations) {
		return locations.split("\\s*,\\s*");
	}

	private static final class FlywayDependsOnPostProcessor implements BeanFactoryPostProcessor {
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			if (!beanFactory.containsBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN)) {
				return;
			}
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN);
			beanDefinition.setDependsOn(append(beanDefinition.getDependsOn(), FLYWAY_MIGRATION_BEAN));
		}

		private static String[] append(String[] values, String value) {
			if (values == null || values.length == 0) {
				return new String[] {value};
			}
			for (String existing : values) {
				if (value.equals(existing)) {
					return values;
				}
			}
			String[] result = new String[values.length + 1];
			System.arraycopy(values, 0, result, 0, values.length);
			result[values.length] = value;
			return result;
		}
	}
}
