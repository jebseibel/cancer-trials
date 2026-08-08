# MySQL user for n8n access

n8n runs in Docker, so it reaches MySQL over the Docker bridge network rather than from
`localhost`. A user scoped to `'cancer_user'@'localhost'` will not authenticate from the
container — hence recreating it with the `'%'` host wildcard.

**Run these yourself.** The user owns the database; never execute these from an assistant
session.

```sql
DROP USER 'cancer_user'@'localhost';
CREATE USER 'cancer_user'@'%' IDENTIFIED BY 'PASSWORD';
GRANT ALL PRIVILEGES ON cancer.* TO 'cancer_user'@'%';
FLUSH PRIVILEGES;
```

- Replace `PASSWORD` with the value of `RDS_PASSWORD` from `.env`. Do not commit the real one.
- The first line fails harmlessly if no `@'localhost'` user exists — `CREATE USER` is the part
  that matters.
- Database and user names come from `RDS_DB_NAME` / `RDS_USERNAME` in `.env`; adjust if yours
  differ.

⚠️ **`'%'` means this account accepts connections from any host.** That is acceptable only
because MySQL's port is not exposed beyond the machine. If port 3306 ever becomes reachable
externally, scope this to the Docker subnet instead — see the firewall step in `qa-setup.md`.

This is what makes the `clear-db` webhook (`http://localhost:5678/webhook/clear-db`, GET) work,
which is how the database gets rebuilt in this project.
