pipeline {
    agent any
    
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarqube-token')
        SONAR_HOST_URL = 'http://localhost:9000'
        API_PROJECT_KEY = 'pim-api'
        API_PROJECT_NAME = 'PIM API'
        TEST_PROJECT_KEY = 'pim-tests'
        TEST_PROJECT_NAME = 'PIM Tests'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
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
                    ║ Java: ${sh(script: 'java -version 2>&1 | head -1', returnStdout: true).trim()}
                    ║ Maven: ${sh(script: 'mvn -version | head -1', returnStdout: true).trim()}
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
        
        stage('Ejecutar Pruebas de Integración - API') {
            steps {
                dir('Api') {
                    echo '🔗 Ejecutando pruebas de integración de la API...'
                    sh 'mvn verify -DskipUnitTests'
                }
            }
            post {
                always {
                    dir('Api') {
                        junit testResults: '**/target/failsafe-reports/*.xml', 
                             allowEmptyResults: true
                    }
                }
            }
        }
        
        stage('Ejecutar Pruebas del Sistema') {
            steps {
                dir('test/PIMTest') {
                    echo '🧪 Ejecutando pruebas del sistema PIM...'
                    // Compilar y ejecutar las pruebas de PIMTest
                    sh '''
                        # Verificar si existe pom.xml en este directorio
                        if [ -f "pom.xml" ]; then
                            mvn clean test
                        else
                            echo "⚠️  No se encontró pom.xml en test/PIMTest"
                            echo "ℹ️  Las pruebas del sistema se ejecutarán desde la raíz del proyecto"
                            cd ../..
                            mvn test -Dtest=PIMTest
                        fi
                    '''
                }
            }
            post {
                always {
                    // Publicar resultados de pruebas del sistema
                    junit testResults: '**/test/**/target/surefire-reports/*.xml', 
                         allowEmptyResults: true
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
                            -Dsonar.junit.reportPaths=target/surefire-reports,target/failsafe-reports \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.java.test.binaries=target/test-classes \
                            -Dsonar.exclusions=**/config/**,**/dto/**,**/entity/**,**/*Application.java \
                            -Dsonar.coverage.exclusions=**/config/**,**/dto/**,**/entity/**,**/*Application.java
                        """
                    }
                }
            }
        }
        
        stage('Análisis SonarQube - Tests del Sistema') {
            when {
                expression {
                    fileExists('test/PIMTest/pom.xml')
                }
            }
            steps {
                dir('test/PIMTest') {
                    echo '📊 Ejecutando análisis de SonarQube para las pruebas del sistema...'
                    withSonarQubeEnv('SonarQube-Server') {
                        sh """
                            mvn sonar:sonar \
                            -Dsonar.projectKey=${TEST_PROJECT_KEY} \
                            -Dsonar.projectName='${TEST_PROJECT_NAME}' \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.login=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }
        
        stage('Quality Gate - API') {
            steps {
                echo '⏳ Esperando resultado de Quality Gate de la API...'
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            echo "⚠️  Quality Gate status: ${qg.status}"
                            // No abortar el pipeline, solo advertir
                            unstable(message: "Quality Gate de API falló: ${qg.status}")
                        } else {
                            echo '✅ Quality Gate de API aprobado'
                        }
                    }
                }
            }
        }
        
        stage('Generar Reporte de Cobertura') {
            steps {
                dir('Api') {
                    echo '📈 Generando reporte consolidado de cobertura...'
                    sh 'mvn jacoco:report'
                }
            }
            post {
                always {
                    // Publicar reporte HTML de JaCoCo
                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'Api/target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage Report'
                    ])
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
        
        stage('Análisis de Seguridad - OWASP') {
            when {
                expression {
                    return params.RUN_SECURITY_SCAN == true
                }
            }
            steps {
                dir('Api') {
                    echo '🔒 Ejecutando análisis de seguridad OWASP...'
                    sh 'mvn org.owasp:dependency-check-maven:check'
                }
            }
            post {
                always {
                    dir('Api') {
                        publishHTML(target: [
                            allowMissing: true,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'target',
                            reportFiles: 'dependency-check-report.html',
                            reportName: 'OWASP Dependency Check Report'
                        ])
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
            script {
                def coverageReport = ""
                try {
                    def jacocoReport = readFile('Api/target/site/jacoco/index.html')
                    coverageReport = "\n\n📊 Revisa el reporte de cobertura en: ${env.BUILD_URL}JaCoCo_Coverage_Report/"
                } catch (Exception e) {
                    coverageReport = ""
                }
                
                emailext(
                    subject: "✅ Build Exitoso: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                    body: """
                        <h2>✅ Build completado exitosamente</h2>
                        
                        <table border="1" cellpadding="5">
                            <tr><td><b>Proyecto:</b></td><td>${env.JOB_NAME}</td></tr>
                            <tr><td><b>Build:</b></td><td>#${env.BUILD_NUMBER}</td></tr>
                            <tr><td><b>Duración:</b></td><td>${currentBuild.durationString}</td></tr>
                            <tr><td><b>Branch:</b></td><td>${env.GIT_BRANCH ?: 'N/A'}</td></tr>
                        </table>
                        
                        <h3>📊 Enlaces útiles:</h3>
                        <ul>
                            <li><a href="${env.BUILD_URL}">Build Console Output</a></li>
                            <li><a href="${env.BUILD_URL}testReport/">Test Results</a></li>
                            <li><a href="${env.BUILD_URL}JaCoCo_Coverage_Report/">Coverage Report</a></li>
                            <li><a href="${SONAR_HOST_URL}/dashboard?id=${API_PROJECT_KEY}">SonarQube API Dashboard</a></li>
                        </ul>
                        
                        ${coverageReport}
                    """,
                    mimeType: 'text/html',
                    to: 'equipo@empresa.com',
                    attachLog: false
                )
            }
        }
        
        failure {
            echo '❌ ═══════════════════════════════════════'
            echo '❌   Pipeline falló'
            echo '❌ ═══════════════════════════════════════'
            emailext(
                subject: "❌ Build Fallido: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: """
                    <h2>❌ Build falló</h2>
                    
                    <table border="1" cellpadding="5">
                        <tr><td><b>Proyecto:</b></td><td>${env.JOB_NAME}</td></tr>
                        <tr><td><b>Build:</b></td><td>#${env.BUILD_NUMBER}</td></tr>
                        <tr><td><b>Duración:</b></td><td>${currentBuild.durationString}</td></tr>
                        <tr><td><b>Branch:</b></td><td>${env.GIT_BRANCH ?: 'N/A'}</td></tr>
                    </table>
                    
                    <h3>🔍 Para revisar:</h3>
                    <ul>
                        <li><a href="${env.BUILD_URL}console">Console Output</a></li>
                        <li><a href="${env.BUILD_URL}testReport/">Test Results</a></li>
                    </ul>
                    
                    <p>Por favor revisa los logs para identificar el problema.</p>
                """,
                mimeType: 'text/html',
                to: 'equipo@empresa.com',
                attachLog: true
            )
        }
        
        unstable {
            echo '⚠️  ═══════════════════════════════════════'
            echo '⚠️   Build inestable'
            echo '⚠️  ═══════════════════════════════════════'
            emailext(
                subject: "⚠️  Build Inestable: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: """
                    <h2>⚠️  Build completado con advertencias</h2>
                    
                    <p>El build se completó pero hay problemas de calidad que requieren atención.</p>
                    
                    <table border="1" cellpadding="5">
                        <tr><td><b>Proyecto:</b></td><td>${env.JOB_NAME}</td></tr>
                        <tr><td><b>Build:</b></td><td>#${env.BUILD_NUMBER}</td></tr>
                    </table>
                    
                    <h3>📊 Revisa:</h3>
                    <ul>
                        <li><a href="${SONAR_HOST_URL}/dashboard?id=${API_PROJECT_KEY}">SonarQube Dashboard</a></li>
                        <li><a href="${env.BUILD_URL}testReport/">Test Results</a></li>
                    </ul>
                """,
                mimeType: 'text/html',
                to: 'equipo@empresa.com'
            )
        }
        
        always {
            echo '🧹 Limpiando workspace...'
            script {
                // Limpiar archivos temporales pero mantener reportes
                try {
                    sh '''
                        find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
                        find . -name "*.log" -delete 2>/dev/null || true
                    '''
                } catch (Exception e) {
                    echo "⚠️  Error al limpiar: ${e.message}"
                }
            }
        }
    }
}

// Parámetros del pipeline
parameters {
    booleanParam(
        name: 'RUN_SECURITY_SCAN',
        defaultValue: false,
        description: 'Ejecutar análisis de seguridad OWASP'
    )
    booleanParam(
        name: 'DEPLOY_TO_DEV',
        defaultValue: false,
        description: 'Desplegar automáticamente a desarrollo después del build'
    )
    choice(
        name: 'LOG_LEVEL',
        choices: ['INFO', 'DEBUG', 'WARN'],
        description: 'Nivel de logging para Maven'
    )
}