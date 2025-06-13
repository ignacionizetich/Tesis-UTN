package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.ValidationTokenRepository;
import com.EDJ.ArCash.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping(value = "/validate")
public class TokenController {

    @Autowired
    private ValidationTokenRepository validationTokenRepository;

    @Autowired
    private UserService userService;

    @GetMapping
    public String validateUser(@RequestParam(value = "token", required = false) String tokenValue, Model model) {
        if (tokenValue == null) {
            model.addAttribute("message", "Token no proporcionado");
            return "error-404";
        }

        Optional<ValidationToken> optionalToken = Optional.ofNullable(validationTokenRepository.findByToken(tokenValue));


        if (optionalToken.isEmpty()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Token inválido o no encontrado.");
            return "validate";
        } else {
            ValidationToken token = optionalToken.get();
            if (token.isUsed()) {
                model.addAttribute("success", false);
                model.addAttribute("message", "El token ya fue utilizado.");
                return "validate";
            } else if (token.getExpirationDate().isBefore(LocalDateTime.now())) {
                model.addAttribute("success", false);
                model.addAttribute("message", "El token ha expirado.");
                return "validate";
            } else {
                userService.validarUsuario(token.getUser());
                model.addAttribute("success", true);
                model.addAttribute("message", "Cuenta verificada correctamente.");
                return "validate";
            }
        }
    }

}
