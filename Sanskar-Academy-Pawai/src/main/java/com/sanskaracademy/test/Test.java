package com.sanskaracademy.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sanskaracademy.service.TotpService;
import com.sanskaracademy.service.impl.TotpServiceImpl;

public class Test {

	public static TotpService totpService;
	
	public static void main(String[] args) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	   // System.out.println(encoder.encode("more123!"));
	    System.out.println(encoder.encode("kasare123!"));
	    totpService = new TotpServiceImpl();
	    String secret = totpService.generateSecret();
	    System.out.println("Secret : "+secret);
	    
	    boolean verifycode = totpService.verifyCode(secret, 0);
	    
	    System.out.println("verifycode : "+verifycode);

	}

}
