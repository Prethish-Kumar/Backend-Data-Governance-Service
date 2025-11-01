package com.complyance.Data_Governance_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Helper Class Since Spring Mongo Does Not Show Connection Success Logs.
@SpringBootApplication
public class DataGovernanceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataGovernanceServiceApplication.class, args);
	}

}
