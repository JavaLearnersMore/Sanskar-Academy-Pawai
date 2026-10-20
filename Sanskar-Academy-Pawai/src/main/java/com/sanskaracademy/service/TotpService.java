package com.sanskaracademy.service;

import org.springframework.stereotype.Service;

@Service
public interface TotpService {

	public String generateSecret();
	public boolean verifyCode(String secret, int code);
}
