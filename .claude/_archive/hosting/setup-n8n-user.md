DROP USER 'basic_user'@'localhost';
CREATE USER 'basic_user'@'%' IDENTIFIED BY 'PASSWORD';
GRANT ALL PRIVILEGES ON basic.* TO 'basic_user'@'%';
FLUSH PRIVILEGES;