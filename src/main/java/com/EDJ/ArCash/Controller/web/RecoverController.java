package com.EDJ.ArCash.Controller.web;

import com.EDJ.ArCash.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class RecoverController {

    @Autowired
    private AuthService authService;

    @GetMapping("/forgot")
    public String forgotCredentials(){
        return "recover";
    }

    @GetMapping("/validate-request")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String tokenValue, Model model) {
        // Si el token es válido, muestra recover-password.html
        if (authService.tokenValido(tokenValue)) {
            model.addAttribute("token", tokenValue);
            return "recover-password";
        } else {
            model.addAttribute("error", "Token inválido o expirado.");
            return "reset-password-form";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        String resultado = authService.actualizarPassword(token, password, confirmPassword);

        if (!"Contraseña actualizada correctamente.".equals(resultado)) {
            model.addAttribute("error", resultado);
            model.addAttribute("token", token);
            return "login";
        }

        model.addAttribute("message", resultado);
        return "demo";
    }

}
