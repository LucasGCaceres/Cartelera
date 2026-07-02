export function translateAction(action) {
    const translations = {
        CREATE_PLANT: "Alta de planta",
        UPDATE_PLANT: "Actualización de planta",
        ENABLE_PLANT: "Activación de planta",
        DISABLE_PLANT: "Desactivación de planta",

        CREATE_USER: "Alta de usuario",
        UPDATE_USER: "Actualización de usuario",
        ENABLE_USER: "Activación de usuario",
        DISABLE_USER: "Desactivación de usuario",

        ASSIGN_USER_PLANT_ROLE: "Asignación de permiso",
        UPDATE_USER_PLANT_ROLE: "Cambio de permiso",
        REMOVE_USER_PLANT_ROLE: "Quita de permiso",

        ADD_PLANT_MEMBER: "Alta de miembro de planta",
        UPDATE_PLANT_MEMBER: "Actualización de miembro",
        REMOVE_PLANT_MEMBER: "Baja de miembro de planta",

        CHANGE_AVAILABILITY: "Cambio de disponibilidad",

        ADD_TO_SUCCESSION: "Alta en sucesión",
        REMOVE_FROM_SUCCESSION: "Baja de sucesión",
        MOVE_UP: "Subió en sucesión",
        MOVE_DOWN: "Bajó en sucesión",

        PUBLISH_DISPLAY: "Publicación cartelera",
    };

    return translations[action] || action || "-";
}