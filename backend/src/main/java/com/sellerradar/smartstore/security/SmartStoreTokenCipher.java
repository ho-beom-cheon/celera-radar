package com.sellerradar.smartstore.security;

public interface SmartStoreTokenCipher {
	// TODO: provide an implementation backed by an environment-managed key or KMS before real API integration.
	String encrypt(String rawToken);

	String decrypt(String encryptedToken);
}
