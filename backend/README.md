# Backend de ArCash 

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.5-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Este es el backend para **ArCash**, una billetera virtual desarrollada como proyecto de tesis. Está construido con Java y el framework Spring Boot.

---

##  Descripción

El backend proporciona la lógica de negocio central y la API RESTful para la aplicación ArCash. Se encarga de la autenticación de usuarios, la gestión de cuentas, la realización de transacciones, la gestión de contactos y otras funcionalidades esenciales de una billetera digital.

---

##  Características Principales

* **Autenticación y Seguridad:**
    * Registro de nuevos usuarios con validación por email.
    * Inicio de sesión con credenciales (username/password).
    * Autorización basada en roles (`USER`, `ADMIN`) usando Spring Security.
    * Implementación de JSON Web Tokens (JWT) para la seguridad de los *endpoints*.
    * Flujo de recuperación de contraseña olvidada.

* **Gestión de Cuentas y Usuarios:**
    * Creación automática de cuentas al registrarse.
    * Consulta de datos de usuario y saldos.

* **Operaciones de la Billetera:**
    * Realización de transacciones entre cuentas de ArCash.
    * Historial de transacciones.
    * Gestión de contactos favoritos para transferencias rápidas.

* **Servicios Externos y Tareas:**
    * Integración con `WebClient` para consumir APIs externas (ej. cotización de USD).
    * Cálculo de impuestos (Servicio `TaxService`).
    * Envío de correos transaccionales (Servicio `Resend`).
    * Tareas programadas (`TokenCleanupRunner`) para limpieza de tokens.

---

##  Arquitectura del Proyecto

El proyecto sigue una arquitectura de N capas, separando claramente las responsabilidades:

* **`Config`**: Clases de configuración de Spring (`OpenApiConfig`, `SecurityConfig`, `WebClientConfig`).
* **`Controller`**: La capa de API. Recibe las peticiones HTTP y las delega al servicio correspondiente (`AuthController`, `TransactionController`).
* **`Service`**: La capa de lógica de negocio. El "cerebro" de la aplicación (`AuthService`, `TransactionService`, `UserService`).
* **`Repository`**: La capa de acceso a datos. Interfaces de Spring Data JPA que definen las operaciones de base de datos (`UserRepository`, `AccountRepository`).
* **`DTO` (Data Transfer Objects)**: Objetos planos para transferir datos de forma segura entre la API y la lógica de negocio.
* **`Security`**: Clases dedicadas a la implementación de Spring Security y JWT (`JwtUtils`, `JwtAuthenticationFilter`, `CustomUserDetails`).

---

##  Puesta en Marcha (Desarrollo Local)

### Prerrequisitos

* Tener instalado Java 21 (JDK) o una versión posterior.
* Tener instalado [Maven](https://maven.apache.org/download.cgi).
* Una instancia de MySQL funcionando.

### Instalación y Configuración

1.  **Clonar el repositorio:**
    ```bash
    git clone <url-del-repositorio>
    cd backend
    ```

2.  **Crear la base de datos:**
    Asegúrate de crear una base de datos en tu instancia de MySQL.
    ```sql
    CREATE DATABASE arcash_db;
    ```

3.  **Configurar las variables de entorno:**
    Este proyecto lee su configuración desde `src/main/resources/application.properties`, el cual está preparado para usar variables de entorno.

    Crea un archivo `.env` en la **raíz del proyecto** (en la misma carpeta que `pom.xml`) con los siguientes valores:

    ```bash
    # --- .env ---

    # Configuración de la Base de Datos
    DB_URL=jdbc:mysql://localhost:3306/arcash_db
    DB_USERNAME=tu_usuario_mysql
    DB_PASS=tu_contraseña_mysql

    # Clave secreta para firmar los tokens JWT (debe ser larga y segura)
    SIGNED_JWT=tu_clave_secreta_para_jwt

    # API Key para el servicio de emails (Resend)
    RESEND_API_KEY=tu_api_key_de_resend

    # URL del frontend (para CORS y emails)
    APP_FRONTEND_URL=http://localhost:4200
    ```

    *Nota: El archivo `application.properties` debe estar configurado para leer estas variables, por ejemplo:*
    ```properties
    # --- application.properties ---
    spring.datasource.url=${DB_URL}
    spring.datasource.username=${DB_USERNAME}
    spring.datasource.password=${DB_PASS}
    spring.jwt.secret=${SIGNED_JWT}
    spring.mail.password=${RESEND_API_KEY}
    app.frontend.url=${APP_FRONTEND_URL}

    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
    ```

4.  **Construir el proyecto:**
    ```bash
    mvn clean install
    ```

5.  **Ejecutar la aplicación:**
    ```bash
    mvn spring-boot:run
    ```

La aplicación se iniciará en el puerto por defecto (usualmente `8080`).

---

##  Documentación de la API

Una vez que la aplicación esté corriendo, puedes acceder a la documentación interactiva de la API (generada por Swagger/OpenAPI) en la siguiente URL:

[**http://localhost:8080/swagger-ui.html**](http://localhost:8080/swagger-ui.html)