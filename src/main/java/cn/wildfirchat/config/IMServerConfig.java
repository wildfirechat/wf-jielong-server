package cn.wildfirchat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "im.server")
public class IMServerConfig {
    private String adminUrl;
    private String adminSecret;
}
