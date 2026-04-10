package com.minishop.controller;

import com.minishop.dto.ApiResponse;
import com.minishop.model.Product;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
@Validated
public class MiniShopController {

    private static final String SESSION_USERNAME = "username";
    private static final String SESSION_LOGIN_TIME = "loginTime";
    private static final String SESSION_PROFILE_VIEWS = "profileViews";
    private static final String SESSION_CART = "cart";

    private static final List<Product> PRODUCTS = List.of(
            new Product(1L, "Ao thun basic", new BigDecimal("149000")),
            new Product(2L, "Quan jean slimfit", new BigDecimal("399000")),
            new Product(3L, "Giay sneaker", new BigDecimal("699000")),
            new Product(4L, "Balo mini", new BigDecimal("259000"))
    );

    @GetMapping("/")
    public ResponseEntity<ApiResponse> home(
            HttpServletRequest request,
            @CookieValue(name = "theme", defaultValue = "light") String theme) {
        HttpSession session = request.getSession(false);
        String username = session != null ? (String) session.getAttribute(SESSION_USERNAME) : null;

        String loginMessage = (username == null || username.isBlank())
                ? "Ban chua dang nhap"
                : "Xin chao, " + username;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "Mini Profile App");
        data.put("theme", theme);
        data.put("message", loginMessage);
        data.put("themeHint", "Goi /set-theme/light hoac /set-theme/dark de doi theme");

        return ResponseEntity.ok(ApiResponse.success("Trang chu", data));
    }

    @GetMapping("/set-theme/{theme}")
    public ResponseEntity<ApiResponse> setTheme(
            @PathVariable
            @Pattern(regexp = "light|dark", message = "Theme chi duoc la light hoac dark") String theme,
            HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("theme", theme)
                .httpOnly(false)
                .maxAge(600)
                .path("/")
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("theme", theme);
        data.put("expiresInSeconds", 600);

        return ResponseEntity.ok(ApiResponse.success("Da luu theme vao cookie", data));
    }

    @GetMapping("/login")
    public ResponseEntity<ApiResponse> loginGuide() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "POST");
        data.put("path", "/login");
        data.put("formField", "username");

        return ResponseEntity.ok(ApiResponse.success("Nhap username de dang nhap", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @RequestParam("username") @NotBlank(message = "username khong duoc de trong") String username,
            HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String loginTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        session.setAttribute(SESSION_USERNAME, username.trim());
        session.setAttribute(SESSION_LOGIN_TIME, loginTime);
        session.setAttribute(SESSION_PROFILE_VIEWS, 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", username.trim());
        data.put("loginTime", loginTime);

        return ResponseEntity.ok(ApiResponse.success("Dang nhap thanh cong", data));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> profile(HttpServletRequest request) {
        HttpSession session = getLoggedInSessionOrRedirect(request);
        if (session == null) {
            return redirectToLogin();
        }

        Integer views = (Integer) session.getAttribute(SESSION_PROFILE_VIEWS);
        if (views == null) {
            views = 0;
        }
        views += 1;
        session.setAttribute(SESSION_PROFILE_VIEWS, views);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", session.getAttribute(SESSION_USERNAME));
        data.put("loginTime", session.getAttribute(SESSION_LOGIN_TIME));
        data.put("profileViewCount", views);

        return ResponseEntity.ok(ApiResponse.success("Thong tin trang ca nhan", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .maxAge(0)
                .path("/")
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Dang xuat thanh cong", null));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse> getProducts() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", PRODUCTS.size());
        data.put("items", PRODUCTS);

        return ResponseEntity.ok(ApiResponse.success("Danh sach san pham", data));
    }

    @PostMapping("/cart/add")
    public ResponseEntity<ApiResponse> addToCart(
            @RequestParam("productId") Long productId,
            @RequestParam(name = "quantity", defaultValue = "1") Integer quantity,
            HttpServletRequest request) {
        HttpSession session = getLoggedInSessionOrRedirect(request);
        if (session == null) {
            return redirectToLogin();
        }

        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("quantity phai lon hon 0"));
        }

        Product product = PRODUCTS.stream()
                .filter(item -> item.id().equals(productId))
                .findFirst()
                .orElse(null);

        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Khong tim thay san pham voi productId=" + productId));
        }

        Map<Long, Integer> cart = getOrCreateCart(session);
        int currentQty = cart.getOrDefault(productId, 0);
        cart.put(productId, currentQty + quantity);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productId", productId);
        data.put("productName", product.name());
        data.put("quantityInCart", cart.get(productId));

        return ResponseEntity.ok(ApiResponse.success("Them vao gio hang thanh cong", data));
    }

    @GetMapping("/cart")
    public ResponseEntity<ApiResponse> getCart(HttpServletRequest request) {
        HttpSession session = getLoggedInSessionOrRedirect(request);
        if (session == null) {
            return redirectToLogin();
        }

        Map<Long, Integer> cart = getOrCreateCart(session);
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = PRODUCTS.stream()
                    .filter(item -> item.id().equals(productId))
                    .findFirst()
                    .orElse(null);

            if (product == null) {
                continue;
            }

            BigDecimal subTotal = product.price().multiply(BigDecimal.valueOf(quantity));
            total = total.add(subTotal);

            Map<String, Object> cartItem = new LinkedHashMap<>();
            cartItem.put("productId", product.id());
            cartItem.put("name", product.name());
            cartItem.put("price", product.price());
            cartItem.put("quantity", quantity);
            cartItem.put("subTotal", subTotal);
            items.add(cartItem);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", session.getAttribute(SESSION_USERNAME));
        data.put("items", items);
        data.put("total", total);

        return ResponseEntity.ok(ApiResponse.success("Thong tin gio hang", data));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse> admin(HttpServletRequest request) {
        HttpSession session = getLoggedInSessionOrRedirect(request);
        if (session == null) {
            return redirectToLogin();
        }

        String username = String.valueOf(session.getAttribute(SESSION_USERNAME));
        if (!"admin".equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Ban khong co quyen truy cap trang quan tri"));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", username);
        data.put("message", "Chao mung den trang quan tri");
        data.put("totalProducts", PRODUCTS.size());

        return ResponseEntity.ok(ApiResponse.success("Admin dashboard", data));
    }

    private HttpSession getLoggedInSessionOrRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SESSION_USERNAME) == null) {
            return null;
        }
        return session;
    }

    private ResponseEntity<ApiResponse> redirectToLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/login")
                .body(ApiResponse.error("Chua dang nhap, chuyen huong ve /login"));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getOrCreateCart(HttpSession session) {
        Object existing = session.getAttribute(SESSION_CART);
        if (existing instanceof Map<?, ?> map) {
            return (Map<Long, Integer>) map;
        }

        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(SESSION_CART, cart);
        return cart;
    }
}
