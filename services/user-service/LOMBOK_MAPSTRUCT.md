# 🔧 Lombok + MapStruct - Configuración Corregida

## ❌ Problema: "cannot find symbol"

Este error ocurre cuando Lombok no está correctamente configurado en el procesador de anotaciones de Maven.

## ✅ Solución Aplicada

### 1. **Properties Agregadas**
```xml
<properties>
    <java.version>17</java.version>
    <lombok.version>1.18.30</lombok.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>
```

### 2. **maven-compiler-plugin Configurado**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <annotationProcessorPaths>
            <!-- Lombok processor -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            
            <!-- MapStruct processor -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            
            <!-- Lombok + MapStruct binding -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3. **Dependencias Agregadas**
```xml
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

## 🎯 ¿Por qué era necesario?

### Problema Original
```
❌ Maven compilaba el código sin procesar las anotaciones de Lombok
❌ @Data, @Builder, @NoArgsConstructor no generaban código
❌ Errores: "cannot find symbol" para getters/setters/constructores
```

### Solución
```
✅ maven-compiler-plugin procesa anotaciones en tiempo de compilación
✅ Lombok genera getters/setters/builders automáticamente
✅ MapStruct genera mappers si los usas
✅ lombok-mapstruct-binding asegura compatibilidad entre ambos
```

## 🔨 Compilar el Proyecto

```bash
# Limpiar y compilar
mvn clean compile

# Si sigues teniendo errores, forzar actualización
mvn clean install -U

# Verificar que Lombok funciona
mvn clean compile -X | grep "Lombok"
```

## 🆚 Con vs Sin Configuración

### ❌ SIN maven-compiler-plugin
```java
@Data
public class Usuario {
    private String nombre;
}

// ERROR: cannot find symbol
usuario.getNombre();  // ❌ Método no encontrado
```

### ✅ CON maven-compiler-plugin
```java
@Data
public class Usuario {
    private String nombre;
}

// ✅ FUNCIONA
usuario.getNombre();  // ✅ Lombok generó el getter
usuario.setNombre("Juan");  // ✅ Lombok generó el setter
```

## 📋 Orden de Procesadores (Importante)

El orden en `annotationProcessorPaths` es importante:

```xml
1. Lombok          (primero - genera código base)
2. MapStruct       (segundo - usa código de Lombok)
3. Binding         (tercero - asegura compatibilidad)
```

## 🐛 Troubleshooting

### Error persiste después de agregar configuración

```bash
# 1. Limpiar completamente
mvn clean

# 2. Eliminar carpetas generadas
rm -rf target/
rm -rf .idea/
rm -rf *.iml

# 3. Reimportar en IDE
# IntelliJ: File -> Invalidate Caches -> Restart
# Eclipse: Maven -> Update Project

# 4. Compilar de nuevo
mvn clean compile
```

### IntelliJ IDEA - Habilitar Lombok

```
1. File -> Settings -> Plugins
2. Buscar "Lombok"
3. Instalar "Lombok Plugin"
4. File -> Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors
5. ✅ Enable annotation processing
```

### Eclipse - Habilitar Lombok

```
1. Descargar lombok.jar de https://projectlombok.org/download
2. Ejecutar: java -jar lombok.jar
3. Seleccionar instalación de Eclipse
4. Click "Install/Update"
5. Reiniciar Eclipse
```

### VS Code - Habilitar Lombok

```
1. Instalar extensión "Language Support for Java"
2. Instalar extensión "Lombok Annotations Support"
3. Reload window
```

## ✅ Verificación

Después de aplicar la configuración:

```bash
mvn clean compile
# [INFO] BUILD SUCCESS

# Verificar que se generaron los archivos
ls target/generated-sources/annotations/
```

## 🎁 Bonus: MapStruct

Si decides usar MapStruct para mapear entidades a DTOs:

```java
@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    UsuarioDTO toDTO(Usuario usuario);
    Usuario toEntity(UsuarioDTO dto);
}

// MapStruct genera automáticamente la implementación
```

## 📚 Referencias

- [Lombok](https://projectlombok.org/)
- [MapStruct](https://mapstruct.org/)
- [Lombok + MapStruct](https://github.com/mapstruct/mapstruct-examples/tree/main/mapstruct-lombok)

---

**Nota:** Esta configuración es compatible con Java 17 y Spring Boot 3.2.0
