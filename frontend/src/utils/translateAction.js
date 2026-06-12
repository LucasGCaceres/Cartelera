function translateAction(action) {
    const translations = {
        CREATE_PERSON: "Alta de persona",
        CHANGE_AVAILABILITY: "Cambio disponibilidad",
        MOVE_UP: "Subió en sucesión",
        MOVE_DOWN: "Bajó en sucesión",
        REMOVE_PERSON: "Eliminación",
        PUBLISH_DISPLAY: "Publicación cartelera",
        CREATE_USER: "Alta de usuario",
        UPDATE_USER_ROLE: "Cambio de rol",
        ENABLE_USER: "Activación de usuario",
        DISABLE_USER: "Desactivación de usuario",
        RESET_PASSWORD: "Reseteo de contraseña",
        REGISTER_USER: "Registro de usuario",
    };

    return translations[action] || action;
}