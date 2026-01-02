start java -jar config-server\target\config-server-0.0.1-SNAPSHOT.jar
timeout /t 30 /nobreak

start java -jar service-registry\target\service-registry-0.0.1-SNAPSHOT.jar
timeout /t 20 /nobreak

start java -Dspring.profiles.active=local -jar user-service\target\user-service-0.0.1-SNAPSHOT.jar
timeout /t 10 /nobreak

start java -Dspring.profiles.active=local -jar grievance-service\target\grievance-service-0.0.1-SNAPSHOT.jar
timeout /t 10 /nobreak

start java -Dspring.profiles.active=local -jar feedback-service\target\feedback-service-0.0.1-SNAPSHOT.jar
timeout /t 10 /nobreak

start java -Dspring.profiles.active=local -jar notification-service\target\notification-service-0.0.1-SNAPSHOT.jar
timeout /t 10 /nobreak

start java -Dspring.profiles.active=local -jar reporting-service\target\reporting-service-0.0.1-SNAPSHOT.jar

timeout /t 10 /nobreak
start java -Dspring.profiles.active=local -jar api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar