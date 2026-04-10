# Huong Dan Kiem Thu Mini Shop Backend Bang Postman

## 1. Chuan bi
- Yeu cau da cai JDK 17+ va Maven.
- Mo terminal tai thu muc project va chay:

```bash
mvn spring-boot:run
```

- Backend chay mac dinh tai: `http://localhost:8080`

## 2. Luu y quan trong khi test Session/Cookie
- Trong Postman, tat tuy chon tu xoa cookie (mac dinh Postman se giu lai cookie cho cung domain).
- Session login su dung cookie `JSESSIONID`.
- Theme su dung cookie `theme` co han 10 phut.

## 3. Danh sach API

### 3.1 Trang chu
- Method: `GET`
- URL: `http://localhost:8080/`
- Ket qua mong doi:
  - Luon co title `Mini Profile App`.
  - Neu chua login: message `Ban chua dang nhap`.
  - Neu da login: message `Xin chao, <username>`.
  - Tra ve theme tu cookie (`light`/`dark`).

### 3.2 Dat theme bang cookie
- Method: `GET`
- URL hop le:
  - `http://localhost:8080/set-theme/light`
  - `http://localhost:8080/set-theme/dark`
- Ket qua mong doi:
  - Response header co `Set-Cookie: theme=...; Max-Age=600; Path=/`
  - Goi lai `GET /` thi field `theme` phai dung gia tri vua dat.

#### Test theme khong hop le
- Method: `GET`
- URL: `http://localhost:8080/set-theme/blue`
- Ket qua mong doi: `400 Bad Request`.

### 3.3 Huong dan dang nhap
- Method: `GET`
- URL: `http://localhost:8080/login`
- Ket qua mong doi: JSON huong dan dung `POST /login` voi field `username`.

### 3.4 Dang nhap (tao session)
- Method: `POST`
- URL: `http://localhost:8080/login`
- Body: `x-www-form-urlencoded`
  - Key: `username`
  - Value: vi du `trung`
- Ket qua mong doi:
  - Tra ve `username` va `loginTime`.
  - Postman luu cookie `JSESSIONID`.

### 3.5 Profile (can dang nhap)
- Method: `GET`
- URL: `http://localhost:8080/profile`
- Ket qua mong doi khi da dang nhap:
  - Co `username`
  - Co `loginTime`
  - Co `profileViewCount`

#### Test bo dem F5
- Goi `GET /profile` lien tiep nhieu lan.
- Ket qua mong doi: `profileViewCount` tang dan 1, 2, 3...

#### Test chua dang nhap
- Xoa cookie `JSESSIONID` trong Postman roi goi `GET /profile`.
- Ket qua mong doi:
  - HTTP `302 Found`
  - Header `Location: /login`

### 3.6 Danh sach san pham
- Method: `GET`
- URL: `http://localhost:8080/products`
- Ket qua mong doi: tra ve danh sach san pham mau (`id`, `name`, `price`).

### 3.7 Them vao gio hang
- Method: `POST`
- URL: `http://localhost:8080/cart/add`
- Params (Query Params):
  - `productId` (bat buoc), vi du `1`
  - `quantity` (khong bat buoc, mac dinh `1`)
- Ket qua mong doi khi da dang nhap:
  - Bao them thanh cong
  - Co so luong san pham trong gio sau khi cong don

#### Test loi du lieu
- `quantity=0` -> mong doi `400 Bad Request`
- `productId=999` -> mong doi `404 Not Found`

### 3.8 Xem gio hang
- Method: `GET`
- URL: `http://localhost:8080/cart`
- Ket qua mong doi:
  - Tra ve danh sach item trong gio
  - Co `total` tong tien

### 3.9 Trang quan tri
- Method: `GET`
- URL: `http://localhost:8080/admin`
- Rule:
  - Chua login -> `302` ve `/login`
  - Login username khac `admin` -> `403 Forbidden`
  - Login voi username `admin` -> `200 OK`, vao duoc admin

### 3.10 Dang xuat
- Method: `POST`
- URL: `http://localhost:8080/logout`
- Ket qua mong doi: session bi xoa.
- Response header co `Set-Cookie` de xoa `JSESSIONID` (`Max-Age=0`).

#### Xac nhan logout thanh cong
- Sau `POST /logout`, goi lai `GET /profile`.
- Ket qua mong doi: bi chan, tra `302` va `Location: /login`.

## 4. Goi y tao Postman Collection
Ban co the tao thu muc requests theo thu tu de demo:
1. `GET /`
2. `GET /set-theme/dark`
3. `GET /`
4. `POST /login` (username=trung)
5. `GET /profile`
6. `GET /products`
7. `POST /cart/add?productId=1&quantity=2`
8. `GET /cart`
9. `GET /admin` (se 403 voi user thuong)
10. `POST /logout`
11. `GET /profile` (phai bi chan)

## 5. Luu y mo rong
- Hien tai danh sach san pham dang dung du lieu mau trong code.
- Co the mo rong CRUD san pham, DB that, Spring Security/JWT tuy nhu cau tiep theo.
