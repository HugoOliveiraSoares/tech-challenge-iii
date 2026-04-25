package br.com.fiap.auth.infra.controller;

import br.com.fiap.auth.core.dto.CreateUserInput;
import br.com.fiap.auth.core.dto.CreateUserOutput;
import br.com.fiap.auth.core.dto.UserCredentialsInput;
import br.com.fiap.auth.infra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<CreateUserOutput> createUser(@Valid @RequestBody CreateUserInput input){
        var user = userService.createUser(input);
        URI uri = URI.create("/auth/users/%s".formatted(user.id()));

        return ResponseEntity.created(uri).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> authenticateUser(@Valid @RequestBody UserCredentialsInput credentials){
        var authUser = userService.validateUserCredentials(credentials);
        var token = authUser.userId().toString();

        return ResponseEntity.ok(token);
    }

    @GetMapping("/private")
    public ResponseEntity<String> privateAreaTest(){
        return ResponseEntity.ok("Private area test accessed!");
    }
}
