package com.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectCApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectCApplication.class, args);
        System.out.println("server started");
	}

}
