package com.portfolio.pedidosassistente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PedidosAssistenteApplication {

	public static void main(String[] args) {
		SpringApplication.run(PedidosAssistenteApplication.class, args);
	}

}
