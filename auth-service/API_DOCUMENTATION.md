# API Documentation - Auth Service

## Thông tin chung

### Auth Service (Backend)
- **Service URL**: `http://localhost:9000/identity`
- **Service Port**: `9000` ⭐ **API chạy trên cổng này**
- **Context Path**: `/identity`
- **JWT Issuer**: `edufinai.uth.vn`

### Gateway Service (Optional - cho Frontend)
- **Gateway URL**: `http://localhost:8080`
- **Gateway Port**: `8080`
- **CORS**: Cho phép `http://localhost:3000` (Frontend)

---

## Cách gọi API

### Option 1: Gọi trực tiếp Auth Service (Port 9000) ✅ **KHUYẾN NGHỊ**

**Base URL**: `http://localhost:9000/identity`

**Ưu điểm:**
- Đơn giản, không cần Gateway
- Phù hợp cho development và testing
- Phù hợp cho backend-to-backend communication

**Endpoints:**
- Authentication: `POST http://localhost:9000/identity/auth/token`
- Users: `GET http://localhost:9000/identity/users`
- Roles: `GET http://localhost:9000/identity/roles`
- Permissions: `GET http://localhost:9000/identity/permissions`

**⚠️ Lưu ý:** Nếu gọi từ frontend (browser), có thể gặp CORS error. Trong trường hợp đó, sử dụng Option 2 (Gateway).

---

### Option 2: Gọi qua Gateway (Port 8080)

**Base URL**: `http://localhost:8080`

**Ưu điểm:**
- CORS đã được cấu hình sẵn
- Phù hợp cho frontend (React, Vue, Angular)
- Có thể load balance và service discovery

**Routing qua Gateway:**
- Gateway route: `/auth/**` → `/identity/${segment}`
- Frontend gọi: `http://localhost:8080/auth/**`
- Gateway forward: `lb://AUTH-SERVICE/identity/**`

**Endpoints:**
- Authentication: `POST http://localhost:8080/auth/auth/token` (có prefix `/auth/auth/` vì controller có `@RequestMapping("/auth")`)
- Users: `GET http://localhost:8080/auth/users`
- Roles: `GET http://localhost:8080/auth/roles`
- Permissions: `GET http://localhost:8080/auth/permissions`

**Routing Details:**
- Gateway route: `/auth/**` → `/identity/${segment}`
- Auth Service endpoints:
  - Authentication: `/identity/auth/token`, `/identity/auth/introspect`, etc.
  - Users: `/identity/users`, `/identity/users/{id}`, etc.
  - Roles: `/identity/roles`, etc.
  - Permissions: `/identity/permissions`, etc.

**Vì vậy:**
- Frontend gọi `/auth/auth/token` → Gateway route `/identity/auth/token` ✅
- Frontend gọi `/auth/users` → Gateway route `/identity/users` ✅

---

## Authentication

### JWT Token Flow

**Option 1: Gọi trực tiếp (Port 9000)**
1. **Frontend/Backend gọi login** → `POST http://localhost:9000/identity/auth/token`
2. **Nhận JWT token** từ response
3. **Lưu token** vào localStorage/cookie (frontend) hoặc memory (backend)
4. **Gửi token** trong header mọi request tiếp theo:
   ```
   Authorization: Bearer <token>
   ```

**Option 2: Gọi qua Gateway (Port 8080)**
1. **Frontend gọi login** → `POST http://localhost:8080/auth/auth/token`
2. **Nhận JWT token** từ response
3. **Lưu token** vào localStorage/cookie
4. **Gửi token** trong header mọi request tiếp theo:
   ```
   Authorization: Bearer <token>
   ```
5. **Gateway tự động forward** Authorization header xuống Auth Service

### Cách lưu và sử dụng JWT Token (Frontend)

```javascript
// 1. Sau khi login thành công
const loginResponse = await fetch('http://localhost:8080/auth/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});

const loginData = await loginResponse.json();
const token = loginData.result.token;

// 2. Lưu token vào localStorage
localStorage.setItem('jwt_token', token);

// 3. Sử dụng token trong các request tiếp theo
const token = localStorage.getItem('jwt_token');

const response = await fetch('http://localhost:8080/auth/users/my-info', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

### Public Endpoints (Không cần authentication)

**Option 1: Gọi trực tiếp (Port 9000)**
- `POST http://localhost:9000/identity/users` - Tạo user mới
- `POST http://localhost:9000/identity/auth/token` - Đăng nhập
- `POST http://localhost:9000/identity/auth/introspect` - Kiểm tra token
- `POST http://localhost:9000/identity/auth/logout` - Đăng xuất
- `POST http://localhost:9000/identity/auth/refresh` - Làm mới token

**Option 2: Gọi qua Gateway (Port 8080)**
- `POST http://localhost:8080/auth/users` - Tạo user mới
- `POST http://localhost:8080/auth/auth/token` - Đăng nhập
- `POST http://localhost:8080/auth/auth/introspect` - Kiểm tra token
- `POST http://localhost:8080/auth/auth/logout` - Đăng xuất
- `POST http://localhost:8080/auth/auth/refresh` - Làm mới token

**⚠️ Lưu ý:** Khi gọi qua Gateway, authentication endpoints có prefix `/auth/auth/` vì:
- Gateway route: `/auth/**` → `/identity/${segment}`
- Auth Service AuthenticationController: `@RequestMapping("/auth")`
- Vậy: Frontend `/auth/auth/token` → Gateway `/identity/auth/token` ✅

### Protected Endpoints (Yêu cầu authentication)

Tất cả các endpoint khác yêu cầu JWT token hợp lệ trong header:
```
Authorization: Bearer <token>
```

---

## Response Format

Tất cả các API đều trả về format chuẩn:

```json
{
  "code": 1000,
  "message": "Success message (optional)",
  "result": { ... }
}
```

### Error Response Format

```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

---

## Error Codes

| Code | Message | HTTP Status | Mô tả |
|------|---------|-------------|-------|
| 9999 | Uncategorized error | 500 | Lỗi không xác định |
| 1001 | Uncategorized error | 400 | Lỗi validation không xác định |
| 1002 | User existed | 400 | User đã tồn tại |
| 1003 | Username must be at least {min} characters | 400 | Username không hợp lệ (tối thiểu 4 ký tự) |
| 1004 | Password must be at least {min} characters | 400 | Password không hợp lệ (tối thiểu 6 ký tự) |
| 1005 | User not existed | 404 | User không tồn tại |
| 1006 | Unauthenticated | 401 | Chưa xác thực hoặc token không hợp lệ |
| 1007 | You do not have permission | 403 | Không có quyền truy cập |
| 1008 | Your age must be at least {min} | 400 | Tuổi không hợp lệ |

---

## Authentication APIs

### 1. Đăng nhập (Login)

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/auth/token`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/auth/token`

**Public**: ✅ Yes

**Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true
  }
}
```

**Error Responses**:
- `1005` - User not existed (404)
- `1006` - Unauthenticated (401) - Sai mật khẩu

**Example (JavaScript) - Gọi trực tiếp:**
```javascript
const response = await fetch('http://localhost:9000/identity/auth/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'password123'
  })
});

const data = await response.json();
if (data.code === 1000) {
  localStorage.setItem('jwt_token', data.result.token);
  console.log('Login successful!');
}
```

**Example (JavaScript) - Gọi qua Gateway:**
```javascript
const response = await fetch('http://localhost:8080/auth/auth/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'password123'
  })
});

const data = await response.json();
if (data.code === 1000) {
  localStorage.setItem('jwt_token', data.result.token);
  console.log('Login successful!');
}
```

---

### 2. Kiểm tra Token (Introspect)

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/auth/introspect`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/auth/introspect`

**Public**: ✅ Yes

**Request Body**:
```json
{
  "token": "string"
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "valid": true
  }
}
```

**Error Responses**:
- `1006` - Unauthenticated (401) - Token không hợp lệ hoặc đã hết hạn

**Example (JavaScript) - Gọi trực tiếp:**
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:9000/identity/auth/introspect', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ token })
});

const data = await response.json();
if (data.result.valid) {
  console.log('Token is valid');
} else {
  console.log('Token is invalid or expired');
}
```

**Example (JavaScript) - Gọi qua Gateway:**
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/auth/introspect', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ token })
});

const data = await response.json();
if (data.result.valid) {
  console.log('Token is valid');
} else {
  console.log('Token is invalid or expired');
}
```

---

### 3. Làm mới Token (Refresh Token)

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/auth/refresh`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/auth/refresh`

**Public**: ✅ Yes

**Request Body**:
```json
{
  "token": "string"
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true
  }
}
```

**Error Responses**:
- `1006` - Unauthenticated (401) - Token không hợp lệ hoặc đã hết hạn

**Lưu ý**: Token cũ sẽ bị vô hiệu hóa sau khi refresh.

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/auth/refresh', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ token })
});

const data = await response.json();
if (data.code === 1000) {
  localStorage.setItem('jwt_token', data.result.token);
  console.log('Token refreshed successfully');
}
```

---

### 4. Đăng xuất (Logout)

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/auth/logout`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/auth/logout`

**Public**: ✅ Yes

**Request Body**:
```json
{
  "token": "string"
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": null
}
```

**Lưu ý**: Token sẽ bị vô hiệu hóa và không thể sử dụng lại.

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
await fetch('http://localhost:8080/auth/auth/logout', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ token })
});

localStorage.removeItem('jwt_token');
console.log('Logged out successfully');
```

---

## User APIs

### 1. Tạo User mới

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/users`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/users`

**Public**: ✅ Yes

**Request Body**:
```json
{
  "username": "string",        // Required, min 4 characters
  "password": "string",         // Required, min 6 characters
  "firstName": "string",        // Optional
  "lastName": "string",         // Optional
  "email": "string",            // Optional, must be valid email format
  "phone": "string",            // Optional, min 6, max 20 characters
  "dob": "YYYY-MM-DD"          // Optional, must be at least 10 years old
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "username": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "YYYY-MM-DD",
    "email": "string",
    "phone": "string",
    "roles": []
  }
}
```

**Error Responses**:
- `1002` - User existed (400)
- `1003` - Username must be at least 4 characters (400)
- `1004` - Password must be at least 6 characters (400)
- `1008` - Your age must be at least 10 (400)

**Example (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/auth/users', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'newuser',
    password: 'password123',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    phone: '0123456789',
    dob: '2000-01-01'
  })
});

const data = await response.json();
console.log('User created:', data.result);
```

---

### 2. Lấy danh sách Users

**Option 1: Gọi trực tiếp**
- **Endpoint**: `GET http://localhost:9000/identity/users`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `GET http://localhost:8080/auth/users`

**Public**: ❌ No (Yêu cầu authentication)

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "uuid",
      "username": "string",
      "firstName": "string",
      "lastName": "string",
      "dob": "YYYY-MM-DD",
      "email": "string",
      "phone": "string",
      "roles": [
        {
          "name": "string",
          "description": "string",
          "permissions": [
            {
              "name": "string",
              "description": "string"
            }
          ]
        }
      ]
    }
  ]
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/users', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('Users:', data.result);
```

---

### 3. Lấy thông tin User theo ID

**Option 1: Gọi trực tiếp**
- **Endpoint**: `GET http://localhost:9000/identity/users/{userId}`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `GET http://localhost:8080/auth/users/{userId}`

**Public**: ❌ No (Yêu cầu authentication)

**Path Parameters**:
- `userId` (string, required) - ID của user

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "username": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "YYYY-MM-DD",
    "email": "string",
    "phone": "string",
    "roles": [
      {
        "name": "string",
        "description": "string",
        "permissions": [
          {
            "name": "string",
            "description": "string"
          }
        ]
      }
    ]
  }
}
```

**Error Responses**:
- `1005` - User not existed (404)
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const userId = 'user-uuid-here';
const response = await fetch(`http://localhost:8080/auth/users/${userId}`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('User:', data.result);
```

---

### 4. Lấy thông tin User hiện tại

**Option 1: Gọi trực tiếp**
- **Endpoint**: `GET http://localhost:9000/identity/users/my-info`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `GET http://localhost:8080/auth/users/my-info`

**Public**: ❌ No (Yêu cầu authentication)

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "username": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "YYYY-MM-DD",
    "email": "string",
    "phone": "string",
    "roles": [
      {
        "name": "string",
        "description": "string",
        "permissions": [
          {
            "name": "string",
            "description": "string"
          }
        ]
      }
    ]
  }
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/users/my-info', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('My info:', data.result);
```

---

### 5. Cập nhật User

**Option 1: Gọi trực tiếp**
- **Endpoint**: `PUT http://localhost:9000/identity/users/{userId}`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `PUT http://localhost:8080/auth/users/{userId}`

**Public**: ❌ No (Yêu cầu authentication)

**Path Parameters**:
- `userId` (string, required) - ID của user

**Headers**:
```
Authorization: Bearer <token>
```

**Request Body**:
```json
{
  "password": "string",         // Optional
  "firstName": "string",        // Optional
  "lastName": "string",         // Optional
  "email": "string",            // Optional
  "phone": "string",            // Optional
  "dob": "YYYY-MM-DD",          // Optional, must be at least 18 years old
  "roles": ["string"]           // Optional, array of role names
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "username": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "YYYY-MM-DD",
    "email": "string",
    "phone": "string",
    "roles": [
      {
        "name": "string",
        "description": "string",
        "permissions": [
          {
            "name": "string",
            "description": "string"
          }
        ]
      }
    ]
  }
}
```

**Error Responses**:
- `1005` - User not existed (404)
- `1006` - Unauthenticated (401)
- `1008` - Your age must be at least 18 (400)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const userId = 'user-uuid-here';
const response = await fetch(`http://localhost:8080/auth/users/${userId}`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    firstName: 'Updated Name',
    email: 'newemail@example.com'
  })
});

const data = await response.json();
console.log('User updated:', data.result);
```

---

### 6. Xóa User

**Option 1: Gọi trực tiếp**
- **Endpoint**: `DELETE http://localhost:9000/identity/users/{userId}`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `DELETE http://localhost:8080/auth/users/{userId}`

**Public**: ❌ No (Yêu cầu authentication)

**Path Parameters**:
- `userId` (string, required) - ID của user

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": "User has been deleted"
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const userId = 'user-uuid-here';
const response = await fetch(`http://localhost:8080/auth/users/${userId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log(data.result); // "User has been deleted"
```

---

## Role APIs

### 1. Tạo Role mới

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/roles`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/roles`

**Public**: ❌ No (Yêu cầu authentication)

**Headers**:
```
Authorization: Bearer <token>
```

**Request Body**:
```json
{
  "name": "string",             // Required
  "description": "string",      // Optional
  "permissions": ["string"]     // Optional, array of permission names
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "name": "string",
    "description": "string",
    "permissions": [
      {
        "name": "string",
        "description": "string"
      }
    ]
  }
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/roles', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'ADMIN',
    description: 'Administrator role',
    permissions: ['CREATE_USER', 'DELETE_USER']
  })
});

const data = await response.json();
console.log('Role created:', data.result);
```

---

### 2. Lấy danh sách Roles

**Option 1: Gọi trực tiếp**
- **Endpoint**: `GET http://localhost:9000/identity/roles`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `GET http://localhost:8080/auth/roles`

**Public**: ❌ No (Yêu cầu authentication)

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "name": "string",
      "description": "string",
      "permissions": [
        {
          "name": "string",
          "description": "string"
        }
      ]
    }
  ]
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/roles', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('Roles:', data.result);
```

---

### 3. Xóa Role

**Option 1: Gọi trực tiếp**
- **Endpoint**: `DELETE http://localhost:9000/identity/roles/{role}`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `DELETE http://localhost:8080/auth/roles/{role}`

**Public**: ❌ No (Yêu cầu authentication)

**Path Parameters**:
- `role` (string, required) - Tên của role

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": null
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const roleName = 'ADMIN';
const response = await fetch(`http://localhost:8080/auth/roles/${roleName}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('Role deleted');
```

---

## Permission APIs

### 1. Tạo Permission mới

**Option 1: Gọi trực tiếp**
- **Endpoint**: `POST http://localhost:9000/identity/permissions`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `POST http://localhost:8080/auth/permissions`

**Public**: ❌ No (Yêu cầu authentication)

**Headers**:
```
Authorization: Bearer <token>
```

**Request Body**:
```json
{
  "name": "string",             // Required
  "description": "string"       // Optional
}
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "name": "string",
    "description": "string"
  }
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/permissions', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'CREATE_USER',
    description: 'Permission to create users'
  })
});

const data = await response.json();
console.log('Permission created:', data.result);
```

---

### 2. Lấy danh sách Permissions

**Option 1: Gọi trực tiếp**
- **Endpoint**: `GET http://localhost:9000/identity/permissions`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `GET http://localhost:8080/auth/permissions`

**Public**: ❌ No (Yêu cầu authentication)

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "name": "string",
      "description": "string"
    }
  ]
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const response = await fetch('http://localhost:8080/auth/permissions', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('Permissions:', data.result);
```

---

### 3. Xóa Permission

**Option 1: Gọi trực tiếp**
- **Endpoint**: `DELETE http://localhost:9000/identity/permissions/{permission}`

**Option 2: Gọi qua Gateway**
- **Endpoint**: `DELETE http://localhost:8080/auth/permissions/{permission}`

**Public**: ❌ No (Yêu cầu authentication)

**Path Parameters**:
- `permission` (string, required) - Tên của permission

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": null
}
```

**Error Responses**:
- `1006` - Unauthenticated (401)

**Example (JavaScript)**:
```javascript
const token = localStorage.getItem('jwt_token');
const permissionName = 'CREATE_USER';
const response = await fetch(`http://localhost:8080/auth/permissions/${permissionName}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('Permission deleted');
```

---

## JWT Token Information

### Token Claims

JWT token chứa các thông tin sau:

- `sub`: Username của user
- `iss`: `edufinai.uth.vn`
- `iat`: Thời gian tạo token (Unix timestamp)
- `exp`: Thời gian hết hạn token (Unix timestamp)
- `jti`: JWT ID (unique identifier)
- `scope`: Danh sách roles và permissions (space-separated)

### Token Duration

- **Valid Duration**: 3600 seconds (1 hour) - Thời gian token hợp lệ
- **Refreshable Duration**: 36000 seconds (10 hours) - Thời gian có thể refresh token

### Token Format

Token được ký bằng thuật toán `HS512` và có format:
```
eyJhbGciOiJIUzUxMiJ9.<header>.<signature>
```

---

## Frontend Integration Guide

### 1. Setup API Client (Axios Example)

```javascript
// apiClient.js
import axios from 'axios';

// Option 1: Gọi trực tiếp Auth Service (Port 9000) - KHUYẾN NGHỊ
const API_BASE_URL = 'http://localhost:9000/identity';

// Option 2: Gọi qua Gateway (Port 8080) - Nếu gặp CORS error với Option 1
// const API_BASE_URL = 'http://localhost:8080/auth';

// Tạo axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - tự động thêm token vào header
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - xử lý lỗi 401 (token hết hạn)
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Token hết hạn, thử refresh
      const token = localStorage.getItem('jwt_token');
      if (token) {
        try {
          // Nếu dùng Option 1 (trực tiếp): '/auth/refresh'
          // Nếu dùng Option 2 (gateway): '/auth/auth/refresh'
          const refreshResponse = await axios.post(`${API_BASE_URL}/auth/refresh`, { token });
          const newToken = refreshResponse.data.result.token;
          localStorage.setItem('jwt_token', newToken);
          
          // Retry request ban đầu với token mới
          error.config.headers.Authorization = `Bearer ${newToken}`;
          return axios.request(error.config);
        } catch (refreshError) {
          // Refresh thất bại, redirect về login
          localStorage.removeItem('jwt_token');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 2. Sử dụng API Client

```javascript
// authService.js
import apiClient from './apiClient';

export const authService = {
  // Login
  async login(username, password) {
    // Nếu dùng Option 1 (trực tiếp): '/auth/token'
    // Nếu dùng Option 2 (gateway): '/auth/auth/token'
    const response = await apiClient.post('/auth/token', { username, password });
    const token = response.data.result.token;
    localStorage.setItem('jwt_token', token);
    return response.data;
  },

  // Logout
  async logout() {
    const token = localStorage.getItem('jwt_token');
    // Nếu dùng Option 1 (trực tiếp): '/auth/logout'
    // Nếu dùng Option 2 (gateway): '/auth/auth/logout'
    await apiClient.post('/auth/logout', { token });
    localStorage.removeItem('jwt_token');
  },

  // Get current user info
  async getMyInfo() {
    const response = await apiClient.get('/users/my-info');
    return response.data.result;
  },

  // Get all users
  async getUsers() {
    const response = await apiClient.get('/users');
    return response.data.result;
  }
};
```

### 3. React Hook Example

```javascript
// useAuth.js
import { useState, useEffect } from 'react';
import { authService } from './services/authService';

export const useAuth = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      authService.getMyInfo()
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('jwt_token');
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (username, password) => {
    const data = await authService.login(username, password);
    const userInfo = await authService.getMyInfo();
    setUser(userInfo);
    return data;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  return { user, loading, login, logout };
};
```

---

## Examples

### Example 1: Đăng nhập và lấy thông tin user (cURL)

```bash
# Option 1: Gọi trực tiếp (Port 9000) - KHUYẾN NGHỊ

# 1. Đăng nhập
curl -X POST http://localhost:9000/identity/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'

# Response:
# {
#   "code": 1000,
#   "result": {
#     "token": "eyJhbGciOiJIUzUxMiJ9...",
#     "authenticated": true
#   }
# }

# 2. Lấy thông tin user hiện tại (thay <token> bằng token từ bước 1)
curl -X GET http://localhost:9000/identity/users/my-info \
  -H "Authorization: Bearer <token>"

# Option 2: Gọi qua Gateway (Port 8080)

# 1. Đăng nhập
curl -X POST http://localhost:8080/auth/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'

# 2. Lấy thông tin user hiện tại
curl -X GET http://localhost:8080/auth/users/my-info \
  -H "Authorization: Bearer <token>"
```

### Example 2: Tạo user mới (cURL)

**Option 1: Gọi trực tiếp**
```bash
curl -X POST http://localhost:9000/identity/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "0123456789",
    "dob": "2000-01-01"
  }'
```

**Option 2: Gọi qua Gateway**
```bash
curl -X POST http://localhost:8080/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "0123456789",
    "dob": "2000-01-01"
  }'
```

### Example 3: Kiểm tra token (cURL)

**Option 1: Gọi trực tiếp**
```bash
curl -X POST http://localhost:9000/identity/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**Option 2: Gọi qua Gateway**
```bash
curl -X POST http://localhost:8080/auth/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

---

## Gateway Configuration

### CORS Configuration

Gateway đã được cấu hình để cho phép:
- **Origin**: `http://localhost:3000` (Frontend)
- **Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Headers**: Tất cả headers (bao gồm Authorization)
- **Credentials**: true

### Route Mapping

Gateway route mapping:
- **Frontend path**: `/auth/**`
- **Backend path**: `/identity/**`
- **Service**: `lb://AUTH-SERVICE`

**⚠️ Lưu ý về Routing:**
Gateway route config hiện tại: `/auth/**` → `/identity/${segment}`

Điều này có nghĩa:
- Frontend gọi: `http://localhost:8080/auth/token` → Gateway route: `lb://AUTH-SERVICE/identity/token`
- Frontend gọi: `http://localhost:8080/auth/users` → Gateway route: `lb://AUTH-SERVICE/identity/users`

**Tuy nhiên**, trong Auth Service:
- AuthenticationController có `@RequestMapping("/auth")` → endpoint thực tế: `/identity/auth/token`
- UserController có `@RequestMapping("/users")` → endpoint thực tế: `/identity/users`

**Có thể cần điều chỉnh:**
1. Sửa gateway route để `/auth/**` → `/identity/auth/${segment}` cho authentication endpoints
2. Hoặc tách thành 2 routes: `/auth/auth/**` và `/auth/users/**`, `/auth/roles/**`, etc.
3. Hoặc sửa AuthenticationController để bỏ prefix `/auth`

**Hiện tại documentation giả định routing đã được cấu hình đúng.**

---

## Notes

1. **CORS**: CORS được xử lý bởi Gateway, frontend chỉ cần gọi qua Gateway.
2. **Password**: Password được mã hóa bằng BCrypt với strength 10.
3. **Token Invalidation**: Token bị vô hiệu hóa sau khi logout hoặc refresh.
4. **Validation**: 
   - Username: tối thiểu 4 ký tự
   - Password: tối thiểu 6 ký tự
   - Email: phải đúng format email
   - Phone: từ 6 đến 20 ký tự
   - DOB: tối thiểu 10 tuổi khi tạo, 18 tuổi khi cập nhật
5. **JWT Scope**: Scope trong JWT token chứa roles và permissions dưới dạng space-separated string, ví dụ: `ROLE_ADMIN CREATE_USER DELETE_USER`
6. **Gateway Forward**: Gateway tự động forward `Authorization` header từ client xuống backend services.
7. **Service Discovery**: Gateway sử dụng Eureka để discover services (AUTH-SERVICE).

---

## Troubleshooting

### Vấn đề: CORS error khi gọi từ frontend

**Nguyên nhân:**
- Frontend đang gọi trực tiếp Auth Service thay vì qua Gateway
- Gateway chưa được cấu hình CORS đúng

**Giải pháp:**
- Đảm bảo frontend gọi qua Gateway: `http://localhost:8080/auth/**`
- Kiểm tra CORS config trong gateway `application.yml`
- Đảm bảo `allowedOrigins` có `http://localhost:3000`

### Vấn đề: 401 Unauthenticated khi gọi qua Gateway

**Nguyên nhân:**
- Client không gửi `Authorization` header
- Token đã hết hạn
- Route rewrite không đúng

**Giải pháp:**
1. Kiểm tra request có header `Authorization: Bearer <token>` không
2. Kiểm tra token còn valid không (gọi `/auth/introspect`)
3. Kiểm tra route configuration trong gateway `application.yml`

### Vấn đề: Gateway không route được request

**Nguyên nhân:**
- Eureka service discovery chưa hoạt động
- Service chưa đăng ký với Eureka
- Route configuration sai

**Giải pháp:**
1. Kiểm tra Eureka server đang chạy: `http://localhost:8761`
2. Kiểm tra AUTH-SERVICE đã đăng ký với Eureka chưa
3. Kiểm tra route configuration trong gateway `application.yml`
