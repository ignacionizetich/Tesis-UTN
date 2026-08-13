package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.AdminRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.result.AdminCreateResult;
import java.util.List;

public interface AdminService {
    public List<UserResponse> getAuthUsers();

    public void disableUser(Long userId);

    public void enableUser(Long userId);

    public AdminCreateResult createAdmin(AdminRequest adminRequest);

    public void cargarAdmin(User user);

    public boolean existsByUsername(String username);

    public boolean existsByEmail(String email);

    public boolean existsByDni(String dni);

}
