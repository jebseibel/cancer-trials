# QA Deployment Guide

## Quick Commands

a. Update the build gradle version number (line 9)
b. Make sure SecurityOPtions are closed

```bash
# 1. Tail the logs
ssh viro-qa "tail -f /var/log/viro/app.log"

# 2. Stop the service
ssh viro-qa "systemctl stop viro"

# 3. Backup database
ssh viro-qa 'source /opt/viro/.env && mysqldump --no-tablespaces -u viro_user -p"$RDS_PASSWORD" viro > /opt/viro/db_backup/backup-$(date +%Y-%m-%d_%H-%M).sql'

# 4. Build and deploy backend
cd /home/jeb/projects/viro/viro-server && ./gradlew clean build -x test && scp build/libs/viro-server-*-SNAPSHOT.jar viro-qa:/opt/viro/viro-server.jar

# 5. Build and deploy frontend
cd /home/jeb/projects/viro/viro-server/frontend && npx vite build && scp -r dist/* viro-qa:/var/www/viro/

# 6. Drop and rebuild database
ssh viro-qa 'source /opt/viro/.env && mysql -u viro_user -p"$RDS_PASSWORD" -e "DROP DATABASE viro; CREATE DATABASE viro;"'
ssh viro-qa 'sudo mysql -u root -e "GRANT ALL PRIVILEGES ON viro.* TO '\''viro_user'\''@'\''localhost'\''; FLUSH PRIVILEGES;"'

# 7. Restart service
ssh viro-qa "systemctl restart viro"

# 8. Copy JAR to releases directory (archive the deployed version)
scp build/libs/viro-server-*-SNAPSHOT.jar viro-qa:/opt/viro/releases/
```
