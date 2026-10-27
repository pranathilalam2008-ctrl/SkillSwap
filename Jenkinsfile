
pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21.0.12.1'
        DEPLOY_DIR = 'C:\\SkillSwap-Deploy'
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

        stage('Deploy SkillSwap') {
            steps {
                bat '''
                    @echo off
                    if not exist "%DEPLOY_DIR%" mkdir "%DEPLOY_DIR%"
                    copy /Y "target\\demo-0.0.1-SNAPSHOT.jar" "%DEPLOY_DIR%\\skillswap.jar"
                    if errorlevel 1 exit /b 1
                    echo SkillSwap JAR deployed successfully.
                '''
            }
        }

        stage('Start SkillSwap') {
            steps {
                withCredentials([
                    string(credentialsId: 'skillswap-db-password', variable: 'DB_PASSWORD')
                ]) {
                    bat '''
                        @echo off
                        powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $existing=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue; if($existing){ throw 'Port 8081 is already occupied. Stop the existing application before deployment.' }; $env:JENKINS_NODE_COOKIE='dontKillMe'; Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\\java.exe') -ArgumentList '-jar','skillswap.jar' -WorkingDirectory $env:DEPLOY_DIR -RedirectStandardOutput (Join-Path $env:DEPLOY_DIR 'app.log') -RedirectStandardError (Join-Path $env:DEPLOY_DIR 'error.log'); Write-Output 'SkillSwap start command submitted.'"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'SkillSwap CI/CD Pipeline SUCCESS!'
        }
        failure {
            echo 'SkillSwap CI/CD Pipeline FAILED!'
        }
    }
}
