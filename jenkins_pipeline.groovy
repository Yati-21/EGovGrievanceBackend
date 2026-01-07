pipeline {
    agent any
    tools 
    {
        jdk 'jdk21'
        maven 'Maven'
    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/Yati-21/EGovGrievanceBackend.git',
                credentialsId: 'github-pat'
            }
        }
        
        stage('Debug Java & Maven') {
            steps {
                bat '''
                java -version
                mvn -version
                where java
                '''
            }
        }
        stage('Package all') {
            steps {
                bat "mvn package -DskipTests"
                
            }
        }
        
        // stage('Package Service Registry') {
        //     steps {
        //         dir('service-registry') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package Config Server') {
        //     steps {
        //         dir('config-server') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package API Gateway') {
        //     steps {
        //         dir('api-gateway') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package User Service') {
        //     steps {
        //         dir('user-service') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package Grievance Service') {
        //     steps {
        //         dir('grievance-service') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package feedback Service') {
        //     steps {
        //         dir('feedback-service') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package Notification Service') {
        //     steps {
        //         dir('notification-service') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }

        // stage('Package Reporting Service') {
        //     steps {
        //         dir('reporting-service') {
        //             bat "mvn package -DskipTests"
        //         }
        //     }
        // }
        

        stage('SonarQube integration') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'SONAR-TOKEN',
                    usernameVariable: 'SONAR_USER',
                    passwordVariable: 'SONAR_TOKEN'
                )]) {
                    bat """
                    mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                    -Dsonar.projectKey=Yati-21_EGovGrievanceBackend ^
                    -Dsonar.token=%SONAR_TOKEN%
                    """
                }
            }
        }
        
        stage('Deploy with Docker') 
        {
            steps 
            {
                withCredentials([file(credentialsId: 'docker-env', variable: 'ENV_FILE')]) 
                {
                    bat """
                    copy %ENV_FILE% .env
                    docker compose up -d --build
                    """
                }
            }
        }

        
    }
}