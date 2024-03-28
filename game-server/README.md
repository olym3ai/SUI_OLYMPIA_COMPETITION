## 1.  Client - Server Protocols
   Định dạng tin nhắn giữa client-server sẽ là text/json
 - Login : text data từ client tới server: "LOGIN-<userId>", vd: LOGIN-93asd
 - LoginSuccess : json data từ server tới client:
   ```json
    {"sessionId": "<sessionIdValue>"}
 - ```