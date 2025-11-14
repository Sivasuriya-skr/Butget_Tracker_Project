package com.budget.backend.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private Upload upload = new Upload();
    
    public Upload getUpload() {
        return upload;
    }
    
    public void setUpload(Upload upload) {
        this.upload = upload;
    }
    
    public static class Upload {
        private String dir = "uploads";
        
        public String getDir() {
            return dir;
        }
        
        public void setDir(String dir) {
            this.dir = dir;
        }
    }
}
