package com.college.elective;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 高校选修课管理系统启动类。
 *
 * @author CollegeElectiveSystem
 */
@Slf4j
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan("com.college.elective.mapper")
@SpringBootApplication
public class CollegeElectiveApplication {

    public static void main(String[] args) throws UnknownHostException {
        Environment env = SpringApplication.run(CollegeElectiveApplication.class, args).getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String host = InetAddress.getLocalHost().getHostAddress();
        log.info("""
                        
                        ----------------------------------------------------------
                        高校选修课管理系统启动成功！
                        本地地址:   http://localhost:{}{}
                        外部地址:   http://{}:{}{}
                        接口文档:   http://localhost:{}{}/doc.html
                        当前环境:   {}
                        ----------------------------------------------------------""",
                port, contextPath, host, port, contextPath, port, contextPath,
                String.join(",", env.getActiveProfiles()));
    }
}
