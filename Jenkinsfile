pipeline {
    agent any
    
    environment {
        // Rutas de Maven y Java
        MAVEN_HOME = 'C:/Users/usuaario/Downloads/apache-maven-3.9.11-bin/apache-maven-3.9.11'
        JAVA_HOME = 'C:/Program Files/Java/jdk-17'
        PATH = "${MAVEN_HOME}/bin;${JAVA_HOME}/bin;${env.PATH}"
        
        // Configuración de SonarQube
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonarqube-token')
        API_PROJECT_KEY = 'sistema-pim'
    
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
        
        stage('🧹 Limpiar') {
            steps {
                dir('Api') {
                    echo 'Limpiando builds anteriores...'
                    bat 'mvn clean'
                }
            }
        }
        
        stage('🔨 Compilar') {
            steps {
                dir('Api') {
                    echo 'Compilando el proyecto...'
                    bat 'mvn compile'
                }
            }
        }
        
        stage('🧪 Ejecutar Pruebas') {
            steps {
                dir('Api') {
                    echo 'Ejecutando pruebas unitarias y generando cobertura...'
                    bat '''
                        mvn test
                    '''
                }
            }
            post {
                always {
                    dir('Api') {
                        script {
                            echo '📊 Procesando resultados de pruebas...'
                            
                            // Publicar resultados de pruebas JUnit
                            def testResults = findFiles(glob: '**/target/surefire-reports/TEST-*.xml')
                            if (testResults.length > 0) {
                                junit testResults: '**/target/surefire-reports/TEST-*.xml',
                                     allowEmptyResults: true,
                                     healthScaleFactor: 1.0
                                echo "✅ Publicados ${testResults.length} archivos de resultados de pruebas"
                            } else {
                                echo '⚠️ No se encontraron resultados de pruebas JUnit'
                            }
                            
                            // Publicar cobertura JaCoCo
                            def jacocoExec = findFiles(glob: '**/target/jacoco.exec')
                            if (jacocoExec.length > 0) {
                                jacoco execPattern: '**/target/jacoco.exec',
                                       classPattern: '**/target/classes',
                                       sourcePattern: '**/src/main/java',
                                       exclusionPattern: '**/config/**,**/dto/**,**/entity/**,**/*Application.class'
                                echo '✅ Reporte de cobertura JaCoCo publicado'
                            } else {
                                echo '⚠️ No se encontró archivo jacoco.exec'
                                echo 'Esto es normal si no existen pruebas unitarias'
                            }
                            
                            // Verificar si hay reportes HTML de JaCoCo
                            def jacocoHtml = findFiles(glob: '**/target/site/jacoco/index.html')
                            if (jacocoHtml.length > 0) {
                                echo '✅ Reporte HTML de cobertura disponible en target/site/jacoco/index.html'
                            }
                        }
                    }
                }
            }
        }
        
        stage('📊 Análisis SonarQube') {
            steps {
                dir('Api') {
                    echo 'Ejecutando análisis de código con SonarQube...'
                    withSonarQubeEnv('SonarQube-Server') {
                        bat """
                            mvn sonar:sonar ^
                            -Dsonar.projectKey=%API_PROJECT_KEY% ^
                            -Dsonar.host.url=%SONAR_HOST_URL% ^
                            -Dsonar.token=%SONAR_TOKEN% ^
                            -Dsonar.qualitygate.wait=false
                        """
                    }
                }
            }
        }
        
        stage('⏳ Quality Gate') {
            steps {
                echo 'Esperando resultado del Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                echo "⚠️ Quality Gate status: ${qg.status}"
                                echo "El build continuará pero se marcará como UNSTABLE"
                                currentBuild.result = 'UNSTABLE'
                            } else {
                                echo '✅ Quality Gate aprobado exitosamente'
                            }
                        } catch (Exception e) {
                            echo "⚠️ Error al verificar Quality Gate: ${e.message}"
                            echo "El build continuará sin Quality Gate..."
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }
        
        stage('📦 Empaquetar') {
            when {
                expression { 
                    currentBuild.result == null || 
                    currentBuild.result == 'SUCCESS' || 
                    currentBuild.result == 'UNSTABLE' 
                }
            }
            steps {
                dir('Api') {
                    echo 'Empaquetando aplicación (sin ejecutar tests nuevamente)...'
                    bat 'mvn package -DskipTests'
                }
            }
            post {
                success {
                    dir('Api') {
                        script {
                            def jarFiles = findFiles(glob: 'target/*.jar')
                            if (jarFiles.length > 0) {
                                archiveArtifacts artifacts: 'target/*.jar',
                                               fingerprint: true,
                                               allowEmptyArchive: false
                                echo "✅ Artefactos empaquetados:"
                                jarFiles.each { file ->
                                    echo "   - ${file.name}"
                                }
                            } else {
                                echo '⚠️ No se encontraron archivos JAR'
                                currentBuild.result = 'UNSTABLE'
                            }
                        }
                    }
                }
            }
        }
    }
    
    post {
        success {
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                echo """
                ═══════════════════════════════════════════════════════
                ✅ PIPELINE COMPLETADO EXITOSAMENTE
                ═══════════════════════════════════════════════════════
                Duración: ${duration}
                Build: #${env.BUILD_NUMBER}
                ═══════════════════════════════════════════════════════
                """
            }
        }
        unstable {
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                echo """
                ═══════════════════════════════════════════════════════
                ⚠️ PIPELINE COMPLETADO CON ADVERTENCIAS
                ═══════════════════════════════════════════════════════
                Duración: ${duration}
                Build: #${env.BUILD_NUMBER}
                Revisa los logs para más detalles
                ═══════════════════════════════════════════════════════
                """
            }
        }
        failure {
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                echo """
                ═══════════════════════════════════════════════════════
                ❌ PIPELINE FALLÓ
                ═══════════════════════════════════════════════════════
                Duración: ${duration}
                Build: #${env.BUILD_NUMBER}
                Revisa los logs para identificar el problema
                ═══════════════════════════════════════════════════════
                """
            }
        }
        always {
            echo 'Finalizando pipeline...'
            // Limpiar workspace si es necesario
            // cleanWs()
        }
    }
}