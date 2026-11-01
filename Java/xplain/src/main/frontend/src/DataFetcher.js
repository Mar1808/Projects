function DataFetcher({ classJava,  errors, correction, xplanation }) {
    return fetch('/data', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ classJava, errors, correction, xplanation}),
    })
        .then(response => {
            if (!response.ok) throw new Error("Erreur lors de l'envoi");
            return response.json();
        })
        .catch(error => {
            console.error("Erreur lors de l'envoi:", error);
            throw error; // Rejette l'erreur pour la gérer dans `App.js`
        });
}

export default DataFetcher;
