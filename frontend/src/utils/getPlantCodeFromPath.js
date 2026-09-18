export function getPlantCodeFromPath(path = window.location.pathname) {
    const parts = path.split("/").filter(Boolean);

    if ((parts[0] === "display" || parts[0] === "app") && parts[1]) {
        return parts[1];
    }

    return null;
}