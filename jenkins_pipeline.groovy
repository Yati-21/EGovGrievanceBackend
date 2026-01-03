pipeline {
    agent any
    tools 
    {
        jdk 'jdk21'
        maven 'Maven'
    }
// pipeline {
//     agent any

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

        
        stage('Package Service Registry') {
            steps {
                dir('service-registry') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package Config Server') {
            steps {
                dir('config-server') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package API Gateway') {
            steps {
                dir('api-gateway') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package User Service') {
            steps {
                dir('user-service') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package Grievance Service') {
            steps {
                dir('grievance-service') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package feedback Service') {
            steps {
                dir('feedback-service') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package Notification Service') {
            steps {
                dir('notification-service') {
                    bat "mvn package -DskipTests"
                }
            }
        }

        stage('Package Reporting Service') {
            steps {
                dir('reporting-service') {
                    bat "mvn package -DskipTests"
                }
            }
        }
        
        // stage('SonarQube integration') {
        //     steps {
        //         script{
        //             sh "mvn -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar '-Dsonar.projectKey=Yati-21' '-Dsonar.token=credentials('SONAR-TOKEN')'"
            
        //         }
        //     }
        // }
        stage('SonarQube integration') 
        {
            steps 
            {
                withCredentials([string(credentialsId: 'SONAR-TOKEN', variable: 'SONAR_TOKEN')]) 
                {
                    bat """
                    mvn -DskipTests verify ^
                    -Dsonar.projectKey=Yati-21 ^
                    -Dsonar.token=%SONAR_TOKEN%
                    """
                }
            }
        }
        
        // stage('Deploy with Docker') {
        //     steps {
        //         script {
        //             withCredentials([file(credentialsId: 'Grievance-docker.env', variable: 'ENV_FILE')]) {
        //                 sh 'cp $docker-env .env'
        //                 sh 'docker-compose up -d --build'
        //             }
        //         }
        //     }
        // }
        
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