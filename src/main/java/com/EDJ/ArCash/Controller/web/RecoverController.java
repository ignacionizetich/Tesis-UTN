package com.EDJ.ArCash.Controller.web;

import com.EDJ.ArCash.Service.AuthService;
import com.EDJ.ArCash.Service.CredentialsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Autowired
    private CredentialsService credentialsService;

    @GetMapping("/forgot")
    public String forgotCredentials(){
        return "recover";
    }

    @Operation(
            summary = "Mostrar formulario de restablecimiento de contraseña",
            description = "Muestra el formulario para restablecer la contraseña si el token es válido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Formulario mostrado o mensaje de error")
    })

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

    @Operation(
            summary = "Restablecer contraseña",
            description = "Permite al usuario restablecer su contraseña usando un token válido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida o mensaje de error")
    })

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        String resultado = credentialsService.actualizarPassword(token, password, confirmPassword);

        if (!"Contraseña actualizada correctamente.".equals(resultado)) {
            model.addAttribute("error", resultado);
            model.addAttribute("token", token);
            return "login";
        }

        model.addAttribute("message", resultado);
        return "home";
    }

}
