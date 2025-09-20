package com.CheckPoint.CheckPoint.Backend.Controller;

import com.CheckPoint.CheckPoint.Backend.Model.User;
import com.CheckPoint.CheckPoint.Backend.Security.JwtUtil;
import com.CheckPoint.CheckPoint.Backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // --- JWT login/register endpoints (unchanged) ---

    // --- Google OAuth2 login ---
    @GetMapping("/oauth2/success")
    public ResponseEntity<?> oauth2LoginSuccess(OAuth2AuthenticationToken token) {
        // Extract info from Google OAuth
        String email = token.getPrincipal().getAttribute("email");
        String name = token.getPrincipal().getAttribute("name");
        String googleId = token.getPrincipal().getAttribute("sub"); // unique Google ID

        // Check if user exists; if not, create new
        User user = userService.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(name.split(" ")[0]);
            newUser.setLastName(name.contains(" ") ? name.substring(name.indexOf(" ") + 1) : "");
            newUser.setGoogleId(googleId);
            newUser.setLoginMethod("GOOGLE");
            return userService.createUserOAuth(newUser);
        });

        // Update user OAuth info
        user.setLastLogin(LocalDateTime.now());
        user.setGoogleId(googleId);
        user.setLoginMethod("GOOGLE");
        userService.updateUser(user);

        // Generate JWT
        String jwt = jwtUtil.generateToken(user);

        // Return JWT and user info
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("user", userResponse(user));
        response.put("message", "Login successful via Google");

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> userResponse(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("firstName", user.getFirstName());
        map.put("lastName", user.getLastName());
        map.put("createdAt", user.getCreatedAt());
        map.put("lastLogin", user.getLastLogin());
        map.put("loginMethod", user.getLoginMethod());
        return map;
    }
}
