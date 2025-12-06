echo ON
java .\DatabaseEncryptor.java
git add .\EncryptedDB2 .\EncryptedDB3
git commit -m "Updated DBV2/3"
git push origin main
del .\EncryptedDB2
del .\EncryptedDB3
pause