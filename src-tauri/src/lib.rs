use tauri::{AppHandle, Emitter};
use std::sync::Mutex;
use once_cell::sync::Lazy;

static APP_HANDLE: Lazy<Mutex<Option<AppHandle>>> = Lazy::new(|| Mutex::new(None));

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            let mut handle = APP_HANDLE.lock().unwrap();
            *handle = Some(app.handle().clone());
            Ok(())
        })
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

#[cfg(target_os = "android")]
#[no_mangle]
pub extern "C" fn Java_com_tauri_quicksave_MainActivity_onFileSaved(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    filename: jni::objects::JString,
    path: jni::objects::JString,
) {
    if let Ok(filename_str) = env.get_string(&filename) {
        if let Ok(path_str) = env.get_string(&path) {
            let filename_string: String = filename_str.into();
            let path_string: String = path_str.into();
            
            if let Ok(handle_opt) = APP_HANDLE.lock() {
                if let Some(app) = handle_opt.as_ref() {
                    #[derive(serde::Serialize, Clone)]
                    struct SavedEvent {
                        fileName: String,
                        path: String,
                    }
                    let _ = app.emit("file-saved", SavedEvent {
                        fileName: filename_string,
                        path: path_string,
                    });
                }
            }
        }
    }
}
