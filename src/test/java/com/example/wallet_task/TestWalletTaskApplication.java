package com.example.wallet_task;

import org.springframework.boot.SpringApplication;

public class TestWalletTaskApplication {

	public static void main(String[] args) {
		SpringApplication.from(WalletTaskApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
