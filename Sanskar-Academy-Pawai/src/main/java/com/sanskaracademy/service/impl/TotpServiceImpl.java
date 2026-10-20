package com.sanskaracademy.service.impl;

import com.sanskaracademy.service.TotpService;
import com.warrenstrange.googleauth.GoogleAuthenticator;

import org.springframework.stereotype.Service;



@Service
public class TotpServiceImpl implements TotpService{
	
	 private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

	@Override
	public String generateSecret() {
        return googleAuthenticator.createCredentials().getKey();
    }

	@Override
	public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

}
