# � PIM (Personal Information Manager)

![Java Version](https://img.shields.io/badge/Java-21-blue)
![JFreeChart](https://img.shields.io/badge/JFreeChart-1.5.6-green)
![Gson](https://img.shields.io/badge/Gson-2.10.1-orange)
![JUnit](https://img.shields.io/badge/JUnit-4.13.2-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

Sistema de Gestión de Información Personal desarrollado bajo una arquitectura Cliente-Servidor, implementando una robusta gestión de datos financieros y personales. 

--- 

## ✨ Características Principales

### 🏗️ Arquitectura
- Diseño Cliente-Servidor para una clara separación de responsabilidades
- API RESTful para la comunicación entre componentes
- Interfaz de usuario nativa en Java Swing
- Base de datos MongoDB para almacenamiento persistente

### 📱 Módulos Principales
- **Gestión Económica**
  - Control de ingresos mensuales
  - Seguimiento de gastos por categorías
  - Cálculo automático de saldo disponible
  - Visualización gráfica de distribución de gastos

- **Información Personal**
  - Gestión de datos personales
  - Sistema de documentación
  - Historial de registros
  - Validación de información

### 🔐 Seguridad
- Autenticación de usuarios
- Validación de correo electrónico único
- Sesiones persistentes
- Encriptación de datos sensibles

## 🛠️ Tecnologías

### Backend
- Java 21
- JFreeChart 1.5.6 (Visualización)
- Gson 2.10.1 (JSON Processing)
- Vavr 0.10.7 (Funcional Programming)
- JUnit 4.13.2 & Hamcrest (Testing)

### Frontend
- Java Swing
- Custom UI Components
- Overpass Font
- Responsive Design

### Herramientas
- NetBeans IDE / VS Code
- Maven (opcional)
- Git 

--- 

## � Estructura del Proyecto

\`\`\`
PIM/
├── src/                       # Código fuente Java
│   ├── app/
│   │   ├── config/           # Configuración
│   │   ├── datos/           # Modelos de datos
│   │   ├── pages/           # Interfaces de usuario
│   │   ├── session/         # Gestión de sesiones
│   │   └── ui/             # Componentes UI
│   └── resources/           # Recursos estáticos
├── Api/                      # Servidor API
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── pom.xml
└── docs/                    # Documentación 
``` 

--- 

## 🚀 Instalación

### Requisitos Previos

1. **JDK 21**
   - [Descargar JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
   - [OpenJDK 21 Alternativa](https://adoptium.net/temurin/releases/?version=21)

2. **Maven** (opcional)
   - [Descargar Maven](https://maven.apache.org/download.cgi)
   - [Guía de instalación](https://maven.apache.org/install.html)

3. **Librerías Requeridas**
   
   📦 **Librerías Principales**
   - [JFreeChart 1.5.6](https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.6/jfreechart-1.5.6.jar) - Para visualizaciones gráficas
   - [Gson 2.10.1](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar) - Para manejo de JSON
   - [Vavr 0.10.7](https://repo1.maven.org/maven2/io/vavr/vavr/0.10.7/vavr-0.10.7.jar) - Funcionalidades funcionales para Java
   - [Overpass Font](https://fonts.google.com/specimen/Overpass) - Para la interfaz gráfica

   📦 **Librerías de Testing**
   - [JUnit 4.13.2](https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar) - Framework de testing
   - [Hamcrest Core 1.3](https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar) - Librería de aserciones para JUnit

### Pasos de Instalación

1. **Clonar el repositorio**
   \`\`\`bash
   git clone https://github.com/bacucan1/PIM.git
   cd PIM
   \`\`\`

2. **Configurar el proyecto**
   - Si usas Maven:
     \`\`\`bash
     mvn clean install
     \`\`\`
   - Si no usas Maven:
     1. Crear una carpeta \`lib\` en la raíz del proyecto
     2. Descargar las librerías mencionadas arriba
     3. Copiar los archivos .jar a la carpeta \`lib\`
     4. Añadir las librerías al classpath del proyecto

3. **Ejecutar la aplicación**
   - Desde NetBeans/VS Code:
     - Abrir el proyecto
     - Ejecutar \`inicio.java\`
   - Desde la línea de comandos:
     \`\`\`bash
     java -cp "lib/*:." app.inicio
     \`\`\` 

--- 

## � Modelo de Datos

### Colecciones MongoDB

#### users
\`\`\`json
{
  "_id": ObjectId,
  "email": String,
  "password": String,
  "created_at": Date
}
\`\`\`

#### financial_info
\`\`\`json
{
  "_id": ObjectId,
  "user_id": ObjectId,
  "ingreso": Double,
  "gastos": {
    "arriendoHipo": Double,
    "services": Double,
    "alimentacion": Double,
    "transporte": Double,
    "otros": Double
  },
  "totales": {
    "totalGastos": Double,
    "disponible": Double
  },
  "timestamp": Date
}
\`\`\`

#### personal_info
\`\`\`json
{
  "_id": ObjectId,
  "user_id": ObjectId,
  "nombreCompleto": String,
  "tipoDocumento": String,
  "numeroDocumento": String,
  "fechaNacimiento": Date,
  "edad": Integer,
  "nacionalidad": String
}
\`\`\`

--- 

## 🧪 Testing

- **Tests Unitarios**: Cobertura de componentes críticos
- **Tests de Integración**: Validación de flujos completos
- **Tests de UI**: Verificación de experiencia de usuario
- **Tests de API**: Validación de endpoints y respuestas

## � Equipo de Desarrollo

**Universidad Libre - Ingeniería de Sistemas**

- Jacobo Arregoces
- Daniel Gonzalez
- Diego Arevalo
- Cristian Gomez
- Eimy Alvarez
- Sergio Merchan
- Andres Urquijo
- Camilo Zuñiga

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor, lee [CONTRIBUTING.md](CONTRIBUTING.md) para más detalles sobre nuestro código de conducta y el proceso para enviarnos pull requests.
