/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vets;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache could be disable in unit test.
 * @author Maciej Szarlinski
 */
@Configuration
@Profile("production")
class PersistenceContext {

    @Bean(destroyMethod = "close")
    DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();

        String hostinfo = System.getenv("MYSQL_HOSTINFO");
        String username = System.getenv("MYSQL_USERNAME");
        String password = System.getenv("MYSQL_PASSWORD");
        
        dataSourceBuilder.url("jdbc:mysql://"+hostinfo+"/petclinic?useUnicode=true");
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        dataSourceBuilder.driverClassName("com.mysql.jdbc.Driver");
        return dataSourceBuilder.build();
    }
}
