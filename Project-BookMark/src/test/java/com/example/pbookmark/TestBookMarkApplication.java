package com.example.pbookmark;

import org.springframework.boot.SpringApplication;

public class TestBookMarkApplication {

	public static void main(String[] args) {
		SpringApplication.from(BookMarkApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
