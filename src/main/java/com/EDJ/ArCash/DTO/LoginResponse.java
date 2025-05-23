package com.EDJ.ArCash.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String AccessToken;
    private String RefreshToken;
    private Long accountId; // 👈 asegurate de tener este campo

public LoginResponse (boolean success, String message, Long accountId){
    this.success = success;
    this.message = message;
    this.accountId = accountId;

}
    public LoginResponse (boolean success, String message){
        this.success = success;
        this.message = message;
    }


}
