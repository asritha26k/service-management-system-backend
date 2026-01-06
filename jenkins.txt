pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    environment {
        // MUST match Jenkins credential ID exactly
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/asritha26k/service-management-system-backend.git'
            }
        }

        stage('Build, Test & SonarCloud') {
            steps {
                bat """
                mvn clean verify ^
                org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                -Dsonar.login=%SONAR_TOKEN% ^
                -Dsonar.projectKey=asritha26k_service-management-system-backend ^
                -Dsonar.organization=asritha26k ^
                -Dsonar.host.url=https://sonarcloud.io ^
                -Dsonar.qualitygate.wait=true
                """
            }
        }

        stage('Build Docker Images') {
            steps {
                bat 'docker-compose build'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}