package cn.wildfirchat;

import cn.wildfirchat.config.IMServerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IMServerConfig.class)
public class CollectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollectionApplication.class, args);
    }
}
