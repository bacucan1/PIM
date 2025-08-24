# 📂  (PIM) - Demo (Java + MongoDB) 

Este proyecto consiste en un **Gestor de Información** desarrollado en **Java** bajo una arquitectura **Cliente-Servidor**, utilizando **MongoDB** como base de datos.   

El sistema está diseñado como una **demo** para mostrar los principios de gestión de información en aplicaciones distribuidas. 

--- 

## 🚀 Características principales 

- Arquitectura **Cliente-Servidor**. 
- Desarrollado en **Java** (aplicación de escritorio, no web). 
- Base de datos **MongoDB** para la persistencia de datos. 
- Gestión de información dividida en dos módulos: 
  - **Información Económica**: ingresos, gastos, saldo. 
  - **Información Personal**: Nombre, edad, teléfono, fecha de nacimiento. 
- Validación de usuarios (correo único). 
- Organización del código en capas: 
  - Cliente (interfaz y peticiones). 
  - Servidor (lógica de negocio). 
  - Repositorio (acceso a datos). 
  - Entidades (Usuario, InfoEconómica, InfoPersonal). 

--- 

## 📂 Estructura del Proyecto 

``` 
PIM/
├── src/                         # Código fuente en Java
│   ├── ApiConfig.java          # Configuración de la API
│   ├── DashBoard.java          # Panel principal de navegación
│   ├── UserSession.java        # Manejo de sesión de usuario
│   ├── Usuario.java            # Modelo de usuario
│   ├── inicio.java             # Ventana principal de inicio
│   ├── login.java             # Formulario de inicio de sesión
│   ├── registro.java          # Formulario de registro
│   ├── pg_info_eco.java       # Panel de información económica
│   ├── pg_info_per.java       # Panel de información personal
│   └── pg_info_res.java       # Panel de resumen de información
├── Api/                        # Servidor API
│   ├── app.py                 # Aplicación del servidor
│   └── test_api.py           # Pruebas de la API
├── nbproject/                  # Configuración del proyecto NetBeans
└── README.md                  # Este archivo 
``` 

--- 

## ⚙️ Requisitos previos 

Antes de ejecutar el proyecto asegúrate de tener instalado: 

- [Java JDK 17+](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)   
- [MongoDB Community Server](https://www.mongodb.com/try/download/community)   
- [MongoDB Java Driver](https://mvnrepository.com/artifact/org.mongodb/mongo-java-driver)   

--- 

## ▶️ Ejecución del proyecto 

1. **Clonar o descomprimir el proyecto:** 
   ```bash 
   git clone https://github.com/bacucan1/PIM 
   cd PIM 
   ``` 

2. **Iniciar el servidor MongoDB:** 
   ```bash 
   mongod 
   ``` 

3. **Iniciar el servidor API Python:**
   ```bash
   cd Api
   python app.py
   ```

4. **Ejecutar la aplicación Java:**
   Abrir el proyecto en NetBeans o VS Code y ejecutar la clase `inicio.java` 
   ``` 

--- 

## 🗃️ Base de datos (MongoDB) 

El proyecto utiliza **3 colecciones** en MongoDB: 

- **Usuario** 
  - `id` (ObjectId) 
  - `nombre` (String) 
  - `correo` (String) 
  - `password` (String) 

- **InfoEconomica** 
  - `id` (ObjectId) 
  - `usuarioId` (ObjectId) 
  - `ingresos` (Double) 
  - `gastos` (Double) 
  - `saldo` (Double) 

- **InfoPersonal** 
  - `id` (ObjectId) 
  - `Nombre` (String) 
  - `nacionalidad` (String) 
  - `edad` (String) 
  - `fechaNacimiento` (Date) 
  - `tipo de documento` (String)
  - `numero de documento` (INT)

--- 

## 🧪 Pruebas realizadas 

- Registro de usuarios (verifica que el correo no se repita). 
- Registro de información económica vinculada a un usuario. 
- Registro de información personal vinculada a un usuario. 
- Consultas de usuarios e información asociada. 
- Validación de conexión a MongoDB. 

--- 

## 📊 UML del Proyecto 

--- 

## 👥 Autores 
- **Jacobo Arregoces** – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Daniel Gonzalez**  – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Diego Arevalo**    – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Cristian Gomez**   – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Eimy Alvarez**     – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Sergio merchan**   – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Andres Urquijo**   – Estudiante de Ingeniería de Sistemas – Universidad Libre   
- **Camilo Zuñiga**    – Estudiante de Ingeniería de Sistemas – Universidad Libre   
