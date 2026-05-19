# FlorSave

Aplicación móvil ligera desarrollada con Tauri, React, TypeScript y Kotlin. Permite interceptar y almacenar de forma local e inmediata archivos, imágenes y documentos compartidos desde cualquier aplicación de Android (como WhatsApp, Discord, Telegram o el navegador web) hacia directorios públicos estructurados, sin necesidad de conexión a internet o servicios externos.

---

## 1. Funcionamiento y Filosofía

FlorSave actúa como un receptor nativo de compartición de archivos en el sistema operativo Android. Su comportamiento está diseñado bajo principios de eficiencia estricta:

* **Compatibilidad Universal (Acción de Compartir)**: Gracias a la definición de filtros de intención (Intent Filters) para el tipo de datos `*/*`, cualquier archivo compartido mediante el menú nativo de Android (ya sea desde WhatsApp, Discord, Telegram, Gmail o almacenamiento local) puede ser procesado por FlorSave.
* **Procesamiento Silencioso (Silent Save)**: Al recibir un archivo compartido, la aplicación inicia un hilo de ejecución en segundo plano (`Dispatchers.IO` en Kotlin) para realizar la copia binaria inmediata del recurso.
* **Retorno Inmediato**: Una vez completado el copiado, la aplicación despliega una notificación flotante corta nativa (`Toast`) con la ruta del archivo y ejecuta la instrucción `finish()`, cerrando la ventana en cuestión de milisegundos y devolviendo al usuario a la interfaz de origen (por ejemplo, el chat de WhatsApp o Discord).
* **Consumo Energético Nulo en Reposo**: No dispone de receptores de arranque (Boot Receivers) ni servicios en segundo plano persistentes. La aplicación se instancia únicamente cuando es invocada por el usuario y se destruye inmediatamente tras completar la operación de guardado.

---

## 2. Destinos de Almacenamiento

El almacenamiento se realiza en directorios estandarizados accesibles por el usuario usando la API `MediaStore` de Android (Scoped Storage), asegurando compatibilidad con las directrices de seguridad de las versiones más recientes del sistema operativo (Android 10 a Android 14+):

* **Imágenes**: Se almacenan en la carpeta pública de imágenes del sistema (`Pictures/FlorSave/`).
* **Documentos (PDF, Word, Excel, ZIP, etc.)**: Se guardan en la carpeta pública de documentos del sistema (`Documents/FlorSave/`).
* **Otros archivos**: Se descargan en el directorio general (`Downloads/FlorSave/`).

*Nota: La aplicación no requiere de permisos de almacenamiento en runtime (`READ_EXTERNAL_STORAGE` o `WRITE_EXTERNAL_STORAGE`) en sistemas operativos modernos debido al uso exclusivo de la API MediaStore.*

---

## 3. Arquitectura Técnica

La aplicación se estructura en base a un modelo híbrido:

1. **Frontend (UI)**: Desarrollado en React y TypeScript con Vite, utilizando la interfaz estética minimalista *Nothing Design* en modo claro, caracterizada por su tipografía monospace industrial, cuadrícula técnica de fondo (`dot-grid`) y énfasis en los contrastes de grises. Se puede acceder a esta interfaz ejecutando la aplicación de forma directa desde el launcher.
2. **Backend (Core)**: Escrito en Rust utilizando el runtime de Tauri v2, encargado de la orquestación y el enlazado simbólico de librerías.
3. **Capa Nativa (Android/Kotlin)**: Localizada en `src-tauri/gen/android/app/src/main/java/com/tauri/quicksave/MainActivity.kt`. Es la responsable directa de interceptar el intent del sistema (`ACTION_SEND` y `ACTION_SEND_MULTIPLE`), resolver las URI de los proveedores de contenido, y realizar la transferencia binaria del archivo.

---

## 4. Requisitos de Compilación

Para generar las compilaciones del proyecto, es necesario disponer del siguiente entorno local:

* **Node.js** (v18 o superior) y npm
* **Rust** (con el target de compilación cruzada para Android instalado mediante `rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android`)
* **Android SDK** (incluyendo Android NDK y las herramientas de plataforma como `adb.exe`)
* **JDK 17 o superior**

---

## 5. Instrucciones para Desarrollo

### Instalación de dependencias
```bash
npm install
```

### Ejecutar entorno de desarrollo en dispositivo físico
```bash
npm run tauri android dev
```

### Compilar APK firmado en modo depuración (Offline)
Genera el paquete ejecutable firmado de manera local con la firma por defecto del SDK de Android para su instalación inmediata por USB:
```bash
npm run tauri android build -- --debug --apk
```

### Instalar manualmente el APK en el dispositivo conectado vía ADB
```powershell
& "C:\Users\<Usuario>\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "src-tauri/gen/android/app/build/outputs/apk/universal/debug/app-universal-debug.apk"
```

