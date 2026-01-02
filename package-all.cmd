cd config-server
call mvn package 
cd ..

cd service-registry
call mvn package 
cd ..

cd user-service
call mvn package -DskipTests
cd ..

cd grievance-service
call mvn package 
cd ..

cd feedback-service
call mvn package 
cd ..

cd notification-service
call mvn package -DskipTests
cd ..

cd reporting-service
call mvn package 
cd ..

cd api-gateway
call mvn package 
cd ..