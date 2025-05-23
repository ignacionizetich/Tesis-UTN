package com.EDJ.ArCash.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrerRequest {
    private String name;
    private String lastName;
    private String dni;
    private String email;
    private String password;
    private String alias;


}
