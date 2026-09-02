# QA Environment Setup Guide

## Purpose

This guide covers the **one-time setup** for provisioning a new QA/demo VPS environment. `qa-deployment.md` covers day-to-day deployment. An earlier version of that file was inherited from another project (wrong host, wrong paths, and a `DROP DATABASE` naming the wrong schema) and was deleted rather than half-corrected; it was rewritten 2026-09-02 from a real deploy against this box instead.

---

## Requirements

| Component | Version |
|-----------|---------|
| OS | Ubuntu 24.04 LTS |
| Java | 21 |
| MySQL | 8.x |
| Node.js | 20+ (build only) |
| Nginx | Latest |

### VPS Recommendations

| Provider | Plan | RAM | Storage | Cost |
|----------|------|-----|---------|------|
| **Hostinger** | KVM 1 | 4GB | 50GB NVMe | $4.99/mo |
| Hetzner | CX22 | 2GB | 40GB | ~$5/mo |
| DigitalOcean | Basic | 2GB | 50GB | ~$12/mo |

**Minimum:** 2GB RAM (tight), **Recommended:** 4GB RAM

---

## Step 1: Configure Local SSH Access

On your **local development machine**, set up SSH for easy access:

### Create SSH Key (if you don't have one)

```bash
ssh-keygen -t rsa -b 4096
# Press Enter to accept defaults
```

### Add SSH Config

Add to `~/.ssh/config`:
```
Host qa
    HostName YOUR_VPS_IP
    User root
    IdentityFile ~/.ssh/id_rsa
```

### Copy Key to Server

```bash
ssh-copy-id root@YOUR_VPS_IP
# Enter the root password when prompted (one-time)
```

Now you can connect with just `ssh qa`.

---

## Step 2: Initial Server Setup

```bash
# SSH into new server
ssh qa

# Update system
apt update && apt upgrade -y

# Install required packages
apt install -y curl wget git unzip
```

---

## Step 3: Install Java 21

```bash
apt install -y openjdk-21-jdk

# Verify
java -version
# Expected: openjdk version "21.x.x"
```

---

## Step 4: Install MySQL 8

```bash
# Install
apt install -y mysql-server

# Start and enable
systemctl start mysql
systemctl enable mysql

# Verify
systemctl status mysql
```

---

## Step 5: Create Database and User

```bash
mysql
```

```sql
CREATE DATABASE cancer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cancer_user'@'localhost' IDENTIFIED BY 'YOUR_SECURE_PASSWORD';
GRANT ALL PRIVILEGES ON cancer.* TO 'cancer_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Important:** Replace `YOUR_SECURE_PASSWORD` with a strong password and record it.

---

## Step 6: Install Node.js 20

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# Verify
node --version  # v20.x.x
npm --version   # 10.x.x
```

---

## Step 7: Install Nginx

```bash
apt install -y nginx
systemctl start nginx
systemctl enable nginx

# Verify - should see nginx welcome page
curl http://localhost
```

---

## Step 8: Create Directories

```bash
mkdir -p /opt/cancer
mkdir -p /var/www/cancer
mkdir -p /var/log/cancer
```

---

## Step 9: Create Environment File

```bash
cat << 'EOF' > /opt/cancer/.env
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=cancer
RDS_USERNAME=cancer_user
RDS_PASSWORD=YOUR_SECURE_PASSWORD
SPRING_PROFILES_ACTIVE=qa
SERVER_PORT=8080
EOF

# Secure the file
chmod 600 /opt/cancer/.env
```

---

## Step 10: Create Systemd Service

```bash
cat << 'EOF' > /etc/systemd/system/cancer.service
[Unit]
Description=Cancer Application
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/cancer
EnvironmentFile=/opt/cancer/.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/cancer/cancer-server.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/cancer/app.log
StandardError=append:/var/log/cancer/error.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable cancer
```

---

## Step 11: Configure Nginx

```bash
cat << 'EOF' > /etc/nginx/sites-available/cancer
server {
    listen 80;
    server_name YOUR_DOMAIN_OR_IP;

    root /var/www/cancer;
    index index.html;

    # Frontend - React SPA
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to Spring Boot
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Swagger UI
    location /swagger-ui/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }

    location /v3/api-docs {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }
}
EOF

# Enable site
ln -sf /etc/nginx/sites-available/cancer /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Test and reload
nginx -t && systemctl reload nginx
```

---

## Step 12: Build Application (Local Machine)

On your development machine:

### Backend JAR

```bash
cd /home/jeb/projects/personal/cancer
./gradlew clean build -x test

# Output: build/libs/cancer-server-*.jar
```

### Frontend

```bash
cd frontend
npm install
npx vite build

# Output: frontend/dist/
```

---

## Step 13: Transfer Files to VPS

From local machine:

```bash
# Transfer JAR
scp build/libs/cancer-server-*-SNAPSHOT.jar root@YOUR_VPS_IP:/opt/cancer/cancer-server.jar

# Transfer frontend
scp -r frontend/dist/* root@YOUR_VPS_IP:/var/www/cancer/
```

---

## Step 14: Start Application

On VPS:

```bash
systemctl start cancer

# Verify
systemctl status cancer

# Check logs
tail -f /var/log/cancer/app.log
```

Wait for "Started Cancer
# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend (via nginx)
curl http://localhost/

# Check API proxy
curl http://localhost/api/actuator/health
```

---

## Step 16: Configure Firewall (Optional but Recommended)

```bash
ufw allow 22/tcp   # SSH
ufw allow 80/tcp   # HTTP
ufw allow 443/tcp  # HTTPS
ufw enable

# Verify
ufw status
```

---

## Step 17: SSL Certificate (Optional)

If you have a domain name:

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d yourdomain.com

# Test auto-renewal
certbot renew --dry-run
```

---

## Setup Complete

Your environment should now be accessible at:

| Service | URL |
|---------|-----|
| Frontend | http://YOUR_IP/ |
| API | http://YOUR_IP/api/ |
| Swagger | http://YOUR_IP/swagger-ui.html |

Day-to-day deployment is `qa-deployment.md`, written from a real deploy rather than adapted from another project.

---

## Security Checklist

### Infrastructure

- [ ] Changed default MySQL root password
- [ ] Set strong password in `/opt/cancer/.env`
- [ ] Configured UFW firewall
- [ ] File permissions on `.env` set to 600
- [ ] MySQL port (3306) not exposed to internet
- [ ] SSH key authentication enabled (password auth disabled)
- [ ] SSL/TLS configured (if domain available)

### Application — blockers, verified in code 2026-08-08

These are not hypothetical hardening suggestions. Each is a live condition in the codebase
today and each one alone is sufficient reason not to expose this beyond localhost.

- [ ] **Restore endpoint security.** `SecurityConfig` currently ends
      `.anyRequest().permitAll()` — **no endpoint requires a token**. The original
      JWT-protected rule set is preserved commented-out directly beneath it. Restore it, but
      keep `/api/uchealth/callback` as `permitAll`: Epic's OAuth redirect cannot carry a JWT.
- [ ] **Replace the JWT signing secret.** There is no `jwt:` block in `application.yml`, so
      `JwtUtil`'s inline default is live — a hardcoded literal committed to the repo. Anyone
      with repo access can forge a valid token. Move it to `JWT_SECRET` in `.env` and generate
      a fresh random value (min 256 bits).
- [ ] **Restrict or remove `UcHealthOAuthTokenController`.** It exposes full CRUD over the
      Epic token table, including reading refresh tokens back over HTTP.
- [ ] **Decide whether `POST /api/auth/register` should be reachable at all.** This is a
      single-patient application; an open registration endpoint is a liability, not a feature.
- [ ] **Confirm Swagger should be public.** The setup above exposes
      `http://YOUR_IP/swagger-ui.html`, which publishes the full API surface.

### Before it holds real patient data

This application is designed around one real person's medical record. Beyond the checklist
above, decide deliberately: who can reach the host, whether the disk is encrypted, what the
backup story is and where backups live, and whether the Epic integration is pointed at the
sandbox or at a real record. None of that is configured by the steps in this guide.
