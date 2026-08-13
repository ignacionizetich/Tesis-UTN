package com.EDJ.ArCash.Service.interfaces;

public interface CardCryptoService {
    public String encrypt(String plain);

    public String decrypt(String encoded);

}
