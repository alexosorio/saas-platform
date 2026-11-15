# 🔧 Configuración Opcional: Eureka Discovery Client

El proyecto está configurado para funcionar **sin Eureka** por defecto. 

## ✅ Estado Actual

- ✅ **Eureka deshabilitado** por defecto
- ✅ El servicio funciona standalone
- ✅ Puede integrarse con Eureka si es necesario

## 📝 Configuración en application.yml

```yaml
eureka:
  client:
    enabled: ${EUREKA_ENABLED:false}  # Deshabilitado por defecto
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

## 🚀 Habilitar Eureka (Opcional)

Si deseas usar Eureka Discovery Client:

### 1. Agregar dependencias en pom.xml

```xml
<properties>
    <java.version>17</java.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
</properties>

<dependencies>
    <!-- Agregar esta dependencia -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Agregar anotación en UserServiceApplication.java

```java
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // Agregar esta línea
@EnableAsync
public class UserServiceApplication {
    // ...
}
```

### 3. Configurar variable de entorno

```bash
# En .env
EUREKA_ENABLED=true
EUREKA_URL=http://localhost:8761/eureka/

# O en docker-compose.yml
environment:
  EUREKA_ENABLED: true
  EUREKA_URL: http://eureka-server:8761/eureka/
```

## 💡 ¿Cuándo usar Eureka?

**Usar Eureka cuando:**
- Tienes múltiples microservicios
- Necesitas service discovery automático
- Usas load balancing entre instancias
- Arquitectura de microservicios distribuidos

**NO usar Eureka cuando:**
- Solo tienes 1-2 servicios
- Usas orquestación con Kubernetes (tiene su propio discovery)
- Desarrollo local simple
- Aplicación monolítica

## ✅ Configuración Actual (Recomendada para empezar)

El proyecto está listo para funcionar **sin Eureka**, lo cual es perfecto para:
- Desarrollo local
- Testing
- Deploy en Docker
- Aplicaciones pequeñas/medianas

Puedes agregarlo más adelante si lo necesitas.
