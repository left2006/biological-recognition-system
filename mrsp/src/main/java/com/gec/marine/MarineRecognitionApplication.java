package com.gec.marine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.gec.marine.mapper")
public class MarineRecognitionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarineRecognitionApplication.class,args);
    }
}