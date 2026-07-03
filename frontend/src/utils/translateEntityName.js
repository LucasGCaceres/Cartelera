export function translateEntityName(entityName) {
    const translations = {
        AppUser: "Usuario",
        Plant: "Planta",
        PlantMember: "Miembro de planta",
        SuccessionOrder: "Sucesión",
        Display: "Cartelera",
        UserPlantRole: "Permiso de planta",
    };

    return translations[entityName] || "Elemento";
}
