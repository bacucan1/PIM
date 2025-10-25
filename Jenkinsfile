pipeline {
    agent any
    
    // Comentar la sección tools si no tienes Maven/JDK configurado
     tools {
         maven 'Maven'
         jdk 'JDK-17'
     }
    
    environment {
        SONAR_TOKEN = credentials('sonarqube-token')
        SONAR_HOST_URL = 'http://localhost:9000'
        API_PROJECT_KEY = 'pim-api'
        API_PROJECT_NAME = 'PIM API'
        
        // Configurar Maven y Java manualmente si no están en tools
        MAVEN_HOME = 'C:\Users\usuaario\Downloads\apache-maven-3.9.11-bin\apache-maven-3.9.11' // Ajusta esta ruta
        JAVA_HOME = 'C:\Program Files\Java\jdk-17' // Ajusta esta ruta
        PATH = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
        stage('Verificar Herramientas') {
            steps {
                echo '🔍 Verificando herramientas instaladas...'
                sh '''
                    echo "Java version:"
                    java -version
                    echo "Maven version:"
                    mvn -version
                    echo "Git version:"
                    git --version
                '''
            }
        }
        
        stage('Checkout') {
            steps {
                echo '📦 Descargando código fuente...'
                checkout scm
            }
        }
        
        stage('Información del Build') {
            steps {
                script {
                    echo """
                    ╔════════════════════════════════════════╗
                    ║         BUILD INFORMATION              ║
                    ╠════════════════════════════════════════╣
                    ║ Job: ${env.JOB_NAME}
                    ║ Build: #${env.BUILD_NUMBER}
                    ║ Branch: ${env.GIT_BRANCH ?: 'N/A'}
                    ╚════════════════════════════════════════╝
                    """
                }
            }
        }
        
        stage('Compilar API') {
            steps {
                dir('Api') {
                    echo '🔨 Compilando la API...'
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }
        
        stage('Ejecutar Pruebas Unitarias - API') {
            steps {
                dir('Api') {
                    echo '🧪 Ejecutando pruebas unitarias de la API...'
                    sh 'mvn test'
                }
            }
            post {
                always {
                    dir('Api') {
                        // Publicar resultados de pruebas JUnit
                        junit testResults: '**/target/surefire-reports/*.xml', 
                             allowEmptyResults: true,
                             healthScaleFactor: 1.0
                        
                        // Publicar reporte de cobertura JaCoCo
                        jacoco(
                            execPattern: '**/target/jacoco.exec',
                            classPattern: '**/target/classes',
                            sourcePattern: '**/src/main/java',
                            exclusionPattern: '**/config/**,**/dto/**,**/entity/**,**/*Application.class'
                        )
                    }
                }
                success {
                    echo '✅ Pruebas unitarias de API completadas exitosamente'
                }
                failure {
                    echo '❌ Algunas pruebas de API fallaron'
                }
            }
        }
        
        stage('Análisis SonarQube - API') {
            steps {
                dir('Api') {
                    echo '📊 Ejecutando análisis de SonarQube para la API...'
                    withSonarQubeEnv('SonarQube-Server') {
                        sh """
                            mvn sonar:sonar \
                            -Dsonar.projectKey=${API_PROJECT_KEY} \
                            -Dsonar.projectName='${API_PROJECT_NAME}' \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.login=${SONAR_TOKEN} \
                            -Dsonar.java.coveragePlugin=jacoco \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -Dsonar.junit.reportPaths=target/surefire-reports \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.java.test.binaries=target/test-classes
                        """
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '⏳ Esperando resultado de Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            echo "⚠️  Quality Gate status: ${qg.status}"
                            unstable(message: "Quality Gate falló: ${qg.status}")
                        } else {
                            echo '✅ Quality Gate aprobado'
                        }
                    }
                }
            }
        }
        
        stage('Empaquetar API') {
            when {
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                dir('Api') {
                    echo '📦 Empaquetando la aplicación...'
                    sh 'mvn package -DskipTests'
                }
            }
            post {
                success {
                    dir('Api') {
                        archiveArtifacts artifacts: 'target/*.jar', 
                                       fingerprint: true,
                                       allowEmptyArchive: false
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ ═══════════════════════════════════════'
            echo '✅   Pipeline ejecutado exitosamente'
            echo '✅ ═══════════════════════════════════════'
        }
        
        failure {
            echo '❌ ═══════════════════════════════════════'
            echo '❌   Pipeline falló'
            echo '❌ ═══════════════════════════════════════'
        }
        
        always {
            echo '🧹 Pipeline completado'
        }
    }
}