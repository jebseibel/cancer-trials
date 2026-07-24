DROP USER 'cancer_user'@'localhost';
CREATE USER 'cancer_user'@'%' IDENTIFIED BY 'PASSWORD';
GRANT ALL PRIVILEGES ON cancer.* TO 'cancer_user'@'%';
FLUSH PRIVILEGES;