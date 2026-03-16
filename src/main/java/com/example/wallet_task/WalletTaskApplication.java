package com.example.wallet_task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class WalletTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletTaskApplication.class, args);
	}

}
