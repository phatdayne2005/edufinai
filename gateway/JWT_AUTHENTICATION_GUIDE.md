# Hướng dẫn JWT Authentication với Gateway

## Tổng quan

Dự án sử dụng **Client-side Storage + Gateway Forward** - cách phổ biến nhất trong microservice architecture.

## Cơ chế hoạt động

### 1. Flow Authentication

```
Client → Gateway → Auth Service
  ↓
Client nhận JWT từ response
  ↓
Client lưu JWT (localStorage/cookie)
  ↓
Client gửi JWT trong header: Authorization: Bearer <token>
  ↓
Gateway tự động forward header xuống backend services
  ↓
Backend services validate JWT và xử lý request
```

### 2. Các bước thực hiện

#### Bước 1: Client gọi login qua Gateway

```bash
POST http://localhost:8080/auth/token
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true
  }
}
```

#### Bước 2: Client lưu JWT

**JavaScript/TypeScript (Frontend):**
```javascript
// Sau khi login thành công
const response = await fetch('http://localhost:8080/auth/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});

const data = await response.json();
const token = data.result.token;

// Lưu vào localStorage
localStorage.setItem('jwt_token', token);

// Hoặc lưu vào cookie (an toàn hơn với httpOnly)
// Cookie sẽ được browser tự động gửi kèm request
```

#### Bước 3: Client gửi JWT trong mọi request tiếp theo

```javascript
// Lấy token từ localStorage
const token = localStorage.getItem('jwt_token');

// Gửi request với Authorization header
const response = await fetch('http://localhost:8080/gamification/challenge', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

#### Bước 4: Gateway tự động forward JWT

Gateway đã được cấu hình để tự động forward `Authorization` header từ client xuống backend services. Không cần thêm code.

## Cấu hình Gateway

### 1. CORS Configuration

Gateway đã được cấu hình để cho phép `Authorization` header:

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedHeaders: 
        - "*"
        - "Authorization"
```

### 2. Route Configuration

```yaml
routes:
  - id: auth-service
    uri: lb://AUTH-SERVICE
    predicates:
      - Path=/auth/**
    filters:
      - name: RewritePath
        args:
          regexp: /auth/?(?<segment>.*)
          replacement: /identity/${segment}
```

### 3. JWT Token Relay Filter

Filter `JwtTokenRelayFilter` đảm bảo Authorization header được forward (Spring Cloud Gateway mặc định đã làm, nhưng filter này cho phép mở rộng sau này).

## Best Practices

### 1. Client-side Storage

**✅ Nên dùng:**
- `localStorage` cho web app (đơn giản, dễ implement)
- `httpOnly cookie` cho web app (an toàn hơn, tránh XSS)
- Secure storage cho mobile app (Keychain/Keystore)

**❌ Không nên:**
- Lưu JWT trong session storage (mất khi đóng tab)
- Lưu JWT trong memory (mất khi refresh)

### 2. Token Refresh

Khi token hết hạn, client cần:
1. Gọi `/auth/refresh` với refresh token
2. Lưu token mới
3. Retry request ban đầu

```javascript
async function makeAuthenticatedRequest(url, options = {}) {
  let token = localStorage.getItem('jwt_token');
  
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`
    }
  });
  
  // Nếu token hết hạn (401), refresh và retry
  if (response.status === 401) {
    const refreshToken = localStorage.getItem('refresh_token');
    const refreshResponse = await fetch('http://localhost:8080/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: refreshToken })
    });
    
    const refreshData = await refreshResponse.json();
    token = refreshData.result.token;
    localStorage.setItem('jwt_token', token);
    
    // Retry request ban đầu
    return fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        'Authorization': `Bearer ${token}`
      }
    });
  }
  
  return response;
}
```

### 3. Logout

Khi logout, client cần:
1. Gọi `/auth/logout` để invalidate token ở server
2. Xóa token khỏi localStorage/cookie

```javascript
async function logout() {
  const token = localStorage.getItem('jwt_token');
  
  await fetch('http://localhost:8080/auth/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ token })
  });
  
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('refresh_token');
}
```

## Testing

### Test với cURL

```bash
# 1. Login
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"password123"}'

# Response sẽ có token, copy token đó

# 2. Gọi API với JWT
curl -X GET http://localhost:8080/gamification/challenge \
  -H "Authorization: Bearer <paste_token_here>"
```

### Test với Postman

1. Tạo request login → Save response token
2. Tạo request khác → Add header: `Authorization: Bearer <token>`
3. Hoặc dùng Postman's "Tests" tab để tự động lưu token:

```javascript
// Trong Tests tab của login request
const response = pm.response.json();
pm.environment.set("jwt_token", response.result.token);
```

```javascript
// Trong Pre-request Script của các request khác
const token = pm.environment.get("jwt_token");
pm.request.headers.add({
    key: "Authorization",
    value: `Bearer ${token}`
});
```

## Troubleshooting

### Vấn đề: 401 Unauthenticated khi gọi qua Gateway

**Nguyên nhân:**
- Client không gửi `Authorization` header
- Token đã hết hạn
- Route rewrite không đúng

**Giải pháp:**
1. Kiểm tra request có header `Authorization: Bearer <token>` không
2. Kiểm tra token còn valid không (gọi `/auth/introspect`)
3. Kiểm tra route configuration trong `application.yml`

### Vấn đề: CORS error

**Nguyên nhân:**
- Gateway chưa cấu hình CORS đúng
- Backend service chưa cấu hình CORS

**Giải pháp:**
- Kiểm tra CORS config trong gateway `application.yml`
- Đảm bảo `allowedHeaders` có `Authorization`

## Tài liệu tham khảo

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OAuth 2.0 Security Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)

