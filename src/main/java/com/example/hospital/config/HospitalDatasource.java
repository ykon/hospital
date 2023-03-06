package com.example.hospital.config;

import javax.sql.DataSource;
import lombok.val;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(
    value = "com.example.hospital.mapper",
    //annotationClass = Mapper.class,
    sqlSessionFactoryRef = "hospitalSqlSessionFactory")
public class HospitalDatasource {

  @Value("${mybatis.type-aliases-package}")
  private String typeAliasPackage;

  @Bean(name = "hospitalDataSource")
  @ConfigurationProperties(prefix = "spring.hospital.datasource")
  public DataSource secondaryDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "hospitalSqlSessionFactory")
  public SqlSessionFactory hospitalSqlSessionFactoryBean(
      @Autowired @Qualifier("hospitalDataSource") DataSource secondaryDataSource, ApplicationContext applicationContext) throws Exception {

    val factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(secondaryDataSource);
    factoryBean.setVfs(SpringBootVFS.class);
    factoryBean.setTypeAliasesPackage(typeAliasPackage);

    val config = new org.apache.ibatis.session.Configuration();
    config.setMapUnderscoreToCamelCase(true);
    factoryBean.setConfiguration(config);
    factoryBean.setMapperLocations(
        applicationContext.getResources("classpath:mybatis/mapper/hospital/**.xml"));
    return factoryBean.getObject();
  }

  @Bean(name = "hospitalSqlSession")
  public SqlSession hospitalSqlSession(
      @Autowired @Qualifier("hospitalSqlSessionFactory") SqlSessionFactory hospitalSqlSessionFactory) {

    return new SqlSessionTemplate(hospitalSqlSessionFactory);
  }

  @Bean(name = "hospitalTransactionManager")
  @Primary
  public DataSourceTransactionManager hospitalTransactionManager(
      @Autowired @Qualifier("hospitalDataSource") DataSource secondaryDataSource) {

    return new DataSourceTransactionManager(secondaryDataSource);
  }
}
