package com.EDJ.ArCash.Service.interfaces;

public interface CardUnlockService {
    public String createUnlock(Long userId);

    public boolean isValid(String token, Long userId);

    public void revoke(String token);

}
