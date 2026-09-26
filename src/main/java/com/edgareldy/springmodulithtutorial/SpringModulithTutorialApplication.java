package com.edgareldy.springmodulithtutorial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the modular monolith: one deployable application made of explicit modules.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Spring Modulith derives the module model from this class: every direct sub-package of its
// package (auth, catalog, customer, order, notification, common) is one application module.
@SpringBootApplication
public class SpringModulithTutorialApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringModulithTutorialApplication.class, args);
	}

}
