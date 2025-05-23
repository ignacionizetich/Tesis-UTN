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

public LoginResponse (boolean success, String message){
    this.success = success;
    this.message = message;
}


}
