# RAG 智能学习平台 - Docker 部署指南

## 前置条件

在服务器上安装 Docker 和 Docker Compose：

### Linux (Ubuntu)
```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash

# 安装 Docker Compose
sudo apt install docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker
```

### Windows Server
下载安装 Docker Desktop for Windows。

---

## 部署步骤

### 1. 把项目传到服务器

```bash
# 在本地打包（如果你在本地操作）
# 然后通过 scp / ftp 传到服务器
scp -r /path/to/test1pj user@服务器IP:~/
```

或者直接在服务器上 Git clone。

### 2. 构建并启动

```bash
# 进入项目目录
cd test1pj

# 构建并启动所有服务（后台运行）
docker compose up -d --build
```

### 3. 查看启动状态

```bash
# 查看所有容器状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 4. 访问网站

打开浏览器访问: `http://服务器IP`

---

## 账号密码

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | zhangmy | 123456 |
| 教师 | liwq | 123456 |
| 教师 | wanglh | 123456 |
| 教师 | chenjf | 123456 |
| 教师 | wuzy | 123456 |
| 教师 | fengh | 123456 |
| 教师 | zhenggd | 123456 |
| 教师 | zhaoyw | 123456 |
| 学生 | student1 | 123456 |
| 学生 | student2 | 123456 |
| 学生 | student3 | 123456 |
| 学生 | student4 | 123456 |
| 学生 | student5 | 123456 |
| 学生 | student6 | 123456 |
| 学生 | student7 | 123456 |
| 学生 | student8 | 123456 |

---

## 常用 Docker 命令

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启单个服务
docker compose restart backend

# 查看日志（实时）
docker compose logs -f

# 查看日志（仅后端）
docker compose logs -f backend

# 完全重建（清除旧数据）
docker compose down -v
docker compose up -d --build

# 进入容器内部调试
docker exec -it znxsgl-backend sh
```

---

## 端口说明

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 | Nginx (前端) | 浏览器直接访问 |
| 8080 | Spring Boot (后端) | API 服务，由 Nginx 转发 |
| 3307 | MySQL | 数据库（外部不可直接访问） |

> MySQL 映射到 3307 端口是为了避免和服务器上已安装的 MySQL 3306 端口冲突。

---

## 数据持久化

MySQL 数据存储在 Docker 卷 `mysql_data` 中，即使 `docker compose down` 也不会丢失数据。

要彻底清除数据，使用: `docker compose down -v`
