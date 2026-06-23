package com.lingdong.payroll.domain.auth;

import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.auth.dto.LoginRequest;
import com.lingdong.payroll.domain.auth.dto.LoginResponse;
import com.lingdong.payroll.domain.user.UserAccountService;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserAccountService userAccountService;

    public AuthController(JwtService jwtService, UserAccountService userAccountService) {
        this.jwtService = jwtService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        CurrentUser user = userAccountService.authenticate(request.username(), request.password());
        String token = jwtService.createToken(user);
        return ApiResponse.ok(new LoginResponse(token, user.id(), user.username(), user.displayName(), user.roleCodes(), user.permissions()));
    }
}
