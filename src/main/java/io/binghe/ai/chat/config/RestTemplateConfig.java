package io.binghe.ai.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description RestTemplateConfig
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
