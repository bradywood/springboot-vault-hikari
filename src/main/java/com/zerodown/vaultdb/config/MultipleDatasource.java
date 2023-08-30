package com.zerodown.vaultdb.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipleDatasource {

    /*
    private DataSource dataSource;


    private DataSourceProperties dataSourceProperties;

    @Autowired
    private void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Autowired
    private void setDataSourceProperties(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }


    @Bean("dataSourcePreChecker")
    @Autowired
//        @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSourcePreChecker(@Qualifier("HikariConfig") HikariConfig existingHikariConfig) {
        HikariConfig newPreCheckHikariConfig = new HikariConfig();
        existingHikariConfig.copyStateTo(newPreCheckHikariConfig);
        newPreCheckHikariConfig.setMaximumPoolSize(2);
        newPreCheckHikariConfig.setIdleTimeout(30);


        return new HikariDataSource(newPreCheckHikariConfig);
    }

     */
}
