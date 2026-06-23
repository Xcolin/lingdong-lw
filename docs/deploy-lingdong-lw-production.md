# 灵动劳务生产部署说明

## 访问地址

- 前端: `https://lingdongdineng.com/lingdong-lw/`
- 后端: `https://lingdongdineng.com/lingdong-lw-server/`
- 后端本机端口: `10002`
- systemd 服务名: `lingdong-lw-server`

## 前端

生产构建会使用 `web/.env.production`:

```env
VITE_BASE_URL=/lingdong-lw/
VITE_API_BASE_URL=/lingdong-lw-server
```

构建后把 `web/dist/` 下的内容上传到服务器:

```text
/home/apps/lingdong-lw/
```

与你的 Nginx 配置对应:

```nginx
location /lingdong-lw/ {
    root /home/apps;
    index index.html;
    try_files $uri $uri/ /lingdong-lw/index.html;
}
```

## 后端

生产环境使用 `prod` profile:

```bash
java -jar payroll-server.jar --spring.profiles.active=prod
```

也可以使用仓库内的 systemd 模板:

```text
deploy/lingdong-lw-server.service
```

部署前必须修改:

- `PAYROLL_DB_USERNAME`
- `PAYROLL_DB_PASSWORD`
- `PAYROLL_JWT_SECRET`
- `PAYROLL_EXPORT_DIR`

与你的 Nginx 配置对应:

```nginx
location /lingdong-lw-server/ {
    proxy_pass http://127.0.0.1:10002;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Authorization $http_authorization;
}
```

后端 `prod` profile 已设置:

```yaml
server:
  port: 10002
  servlet:
    context-path: /lingdong-lw-server
```
