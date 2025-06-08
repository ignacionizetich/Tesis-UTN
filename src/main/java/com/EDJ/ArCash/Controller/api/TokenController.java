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

        String message;

        if (optionalToken.isEmpty()) {

            return "error-404";
        } else {
            ValidationToken token = optionalToken.get();

            if (token.isUsed()) {

                return "error-404";
            } else if (token.getExpirationDate().isBefore(LocalDateTime.now())) {
                return "error-404";
            } else {
                userService.validarUsuario(token.getUser());
                message = "Cuenta verificada correctamente.";
            }
        }

        model.addAttribute("message", message);
        return "validate"; // Thymeleaf buscará validate.html
    }
}
