pipeline {
    agent any
    
    // IMPORTANTE: Configura estas rutas según tu sistema Windows
    environment {
        // Ruta de Maven (usar / en lugar de \)
        MAVEN_HOME = 'C:/Users/usuaario/Downloads/apache-maven-3.9.11-bin/apache-maven-3.9.11'
        
        // Ruta de Java (ajustar según tu instalación)
        JAVA_HOME = 'C:/Program Files/Java/jdk-17'
        
        // Agregar Maven y Java al PATH
        PATH = "${MAVEN_HOME}/bin;${JAVA_HOME}/bin;${env.PATH}"
        
        // Configuración de SonarQube
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonarqube-token')
        API_PROJECT_KEY = 'pim-api'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
        stage('🔍 Verificar Entorno') {
            steps {
                echo 'Verificando herramientas...'
                bat '''
                    echo === Java ===
                    java -version
                    echo.
                    echo === Maven ===
                    mvn -version
                    echo.
                    echo === Directorio actual ===
                    cd
                '''
            }
        }
        
        stage('📦 Checkout') {
            steps {
                echo 'Descargando código fuente...'
                checkout scm
            }
        }
        
        stage('🔨 Compilar API') {
            steps {
                dir('Api') {
                    echo 'Compilando el proyecto...'
                    bat 'mvn clean compile'
                }
            }
        }
        
        stage('🧪 Ejecutar Pruebas') {
            steps {
                dir('Api') {
                    echo 'Ejecutando pruebas unitarias...'
                    bat 'mvn test'
                }
            }
            post {
                always {
                    dir('Api') {
                        // Publicar resultados de pruebas
                        junit testResults: '**/target/surefire-reports/*.xml',
                             allowEmptyResults: true
                        
                        // Publicar cobertura JaCoCo
                        jacoco execPattern: '**/target/jacoco.exec',
                               classPattern: '**/target/classes',
                               sourcePattern: '**/src/main/java'
                    }
                }
            }
        }
        
        stage('📊 Análisis SonarQube') {
            steps {
                dir('Api') {
                    echo 'Ejecutando análisis de código...'
                    withSonarQubeEnv('SonarQube-Server') {
                        bat """
                            mvn sonar:sonar ^
                            -Dsonar.projectKey=%API_PROJECT_KEY% ^
                            -Dsonar.host.url=%SONAR_HOST_URL% ^
                            -Dsonar.login=%SONAR_TOKEN%
                        """
                    }
                }
            }
        }
        
        stage('⏳ Quality Gate') {
            steps {
                echo 'Esperando Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                echo "⚠️ Quality Gate: ${qg.status}"
                                unstable(message: "Quality Gate falló")
                            } else {
                                echo '✅ Quality Gate aprobado'
                            }
                        } catch (Exception e) {
                            echo "⚠️ Error en Quality Gate: ${e.message}"
                            unstable(message: "Error en Quality Gate")
                        }
                    }
                }
            }
        }
        
        stage('📦 Empaquetar') {
            when {
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                dir('Api') {
                    echo 'Empaquetando aplicación...'
                    bat 'mvn package -DskipTests'
                }
            }
            post {
                success {
                    dir('Api') {
                        archiveArtifacts artifacts: 'target/*.jar',
                                       fingerprint: true
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline completado exitosamente'
        }
        failure {
            echo '❌ Pipeline falló'
        }
        always {
            echo 'Limpiando...'
        }
    }
}