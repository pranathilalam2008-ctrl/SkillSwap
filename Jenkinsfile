
pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21.0.12.1'
    }

    stages {
        stage('Verify Java') {
            steps {
                bat '"%JAVA_HOME%\\bin\\java.exe" -version'
            }
        }

        stage('Build SkillSwap') {
            steps {
                bat '''
                    @echo off
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd clean package -DskipTests
                '''
            }
        }

        stage('Verify JAR') {
            steps {
                bat '''
                    @echo off
                    if not exist "target\\demo-0.0.1-SNAPSHOT.jar" (
                        echo ERROR: JAR file not found.
                        exit /b 1
                    )
                    echo SkillSwap JAR created successfully.
                '''
            }
        }
    }

    post {
        success {
            echo 'SkillSwap GitHub Build SUCCESS!'
        }
        failure {
            echo 'SkillSwap Build FAILED!'
        }
    }
}
