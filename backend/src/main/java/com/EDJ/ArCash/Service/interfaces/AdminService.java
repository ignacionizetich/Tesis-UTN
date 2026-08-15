package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.AdminRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.result.AdminCreateResult;
import java.util.List;

public interface AdminService {
     List<UserResponse> getAuthUsers();

     void disableUser(Long userId);

     void enableUser(Long userId);

     AdminCreateResult createAdmin(AdminRequest adminRequest);

     void cargarAdmin(User user);

     boolean existsByUsername(String username);

     boolean existsByEmail(String email);

     boolean existsByDni(String dni);

    void disableAdmin(Long userId);
    
    void enableAdmin(Long userId);

}
