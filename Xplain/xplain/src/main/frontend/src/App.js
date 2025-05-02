import React, {useState, useEffect, useRef} from 'react';
import './App.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import DataFetcher from './DataFetcher';

function App() {
    // Pour le formulaire (et entity -> classe)
    const [javaClass, setJavaClass] = useState('');
    const [selectedVersion, setSelectedVersion] = useState('rapide');

    // historique
    const [history, setHistory] = useState([]); // Liste des objets Explanation

    // navigateur
    const [showEditor, setShowEditor] = useState(false);

    // erreurs ( entity -> errors)
    const [errorCompilation, setErrorCompilation] = useState('');

    // correction (entity -> correction)
    const [showCorrectionButton, setShowCorrectionButton] = useState(false);

    // streaming
    const [tokens, setTokens] = useState([]);
    const [tokensCorrection, setTokensCorrection] = useState([]); // Ajout de l'état pour les tokens de correction

    const [isCorrection, setIsCorrection] = useState(false);
    const [isOpen, setOpen] = useState(false);
    const [isStreamFinished, setIsStreamFinished] = useState(false);
    const ptrEventSource = useRef(null);

    // Récupération des erreurs de compilation
    const fetchCompilationError = () => {
        fetch(`/llm/compile-error?contentClass=${encodeURIComponent(javaClass)}`)
            .then((response) => response.text())
            .then((data) => {
                setErrorCompilation(data);
            })
            .catch((error) => {
                console.error('Erreur lors de la récupération du message d\'erreur :', error);
                setErrorCompilation('Impossible de récupérer le message d\'erreur.');
            });
    };

    const fetchHistory = () => {
        fetch('/history')
            .then(response => response.json())
            .then(data => {
                console.log("Données reçues de /history :", data);
                setHistory(data);
            })
            .catch(error => console.error("Erreur lors de la récupération de l'historique:", error));
    };

    useEffect(() => {
        fetchHistory();
    }, []);

    const save = () => {
        console.log(errorCompilation);
        DataFetcher({
            classJava: javaClass,
            errors: errorCompilation,
            correction: tokensCorrection.join(" "),
            xplanation: tokens.join(" ")
        })
            .then(() => {
                fetchHistory();
                setJavaClass('');
                setSelectedVersion('');
            })
            .catch(error => console.error("Erreur lors de l'envoi:", error));
    };

    const handleClassDoubleClick = (item) => {
        setJavaClass(item.javaClass); // Charge la classe Java directement
        setErrorCompilation(item.error || '...'); // Affiche uniquement les erreurs de la classe sélectionnée
        setTokens([item.xplanation] || ['...']);
        setTokensCorrection([item.correction || ['']])
        setShowEditor(true); // Affiche l'éditeur
    };

    const handleBackToHistory = () => {
        fetchHistory();
        setShowEditor(false); // Retourne à l'historique
    };

    // Fonction pour tronquer les lignes au maximum à trois
    const truncateToThreeLines = (text) => {
        if (!text) return 'Non défini';
        const lines = text.split('\n');
        return lines.slice(0, 3).join('\n') + (lines.length > 3 ? '\n...' : '');
    };

    // partie STREAMING
    const stopSSE = () => {
        console.log("Arrêt du SSE.");
        console.log(errorCompilation);
        ptrEventSource.current.close();
        ptrEventSource.current = null;
        setOpen(false);
        setIsStreamFinished(true);
        setIsCorrection(false);
    };

    useEffect(() => {
        //refresh les erreurs pour pouvoir bien gerer setShowCorrectionButton
        if (errorCompilation !== "") {
            setShowCorrectionButton(true); // Correction activé si erreur
        } else {
            setShowCorrectionButton(false); // Pas de correction si succès
        }
    }, [errorCompilation])

    useEffect(() => {
        // Sauvegarder les données une fois que le stream est terminé
        if (isStreamFinished) {
            save();
        }
    }, [isStreamFinished]);

    const startSSE = () => {
        setIsCorrection(false);
        if(javaClass === null){
            alert("veuillez rentrez une classe java");
            return;
        }
        if (isOpen) {
            console.log("Already listening");
            return;
        }

        fetchCompilationError();
        setTokens([]);
        setTokensCorrection([]);
        setShowCorrectionButton(false); // Masquer Correction pendant l'exécution
        const eventSource = new EventSource(`http://localhost:8080/llm/llm-response?llmSelected=${selectedVersion}&contentClass=${encodeURIComponent(javaClass)}`);
        ptrEventSource.current = eventSource;
        setOpen(true);
        setIsStreamFinished(false);

        eventSource.onopen = () => {
            console.log("Already open");
        };
        eventSource.onmessage = (event) => {
            setTokens((oldToken) => [...oldToken, event.data]);
        };
        eventSource.onerror = () => {
            console.error("ERROR SSE");
            eventSource.close();
            stopSSE();
        };

        eventSource.onclose = () => {
            console.log("Flux finished");
            stopSSE();
        };
    };
    //fin Streaming

    const startCorrectionSSE = () => {
        setIsCorrection(true);
        setTokensCorrection([]);
        setOpen(true); // Désactive START pendant la correction
        setIsStreamFinished(false);

        const eventSourceURL = `http://localhost:8080/llm/llm-correction?contentClass=${encodeURIComponent(javaClass)}&contentErrorMessage=${errorCompilation}&llmSelected=${selectedVersion}`;
        const eventSource = new EventSource(eventSourceURL);

        eventSource.onopen = () => {
            console.log("Connection opened for correction");
        };

        eventSource.onmessage = (event) => {
            setTokensCorrection((oldTokensCorrection) => [...oldTokensCorrection, event.data]);
        };

        eventSource.onerror = () => {
            console.error("ERROR SSE Correction");
            stopSSE();
        };

        eventSource.onclose = () => {
            console.log("Flux de correction terminé");
            stopSSE();
        };

        ptrEventSource.current = eventSource;
    };

    return (
        <div className="App d-flex flex-column">
            {!showEditor ? (
                // Page Historique
                <div
                    className="App-left bg-dark"
                    style={{
                        boxShadow: '0 6px 6px rgba(0, 0, 0, 0.1)',
                    }}
                >
                    <h2 className="text-white align-content-md-center">Historique</h2>
                    <ul
                        className="list-group"
                        style={{
                            maxHeight: '85vh', // La hauteur maximale sera 80% de la hauteur de l'écran
                            overflowY: 'auto', // Active le scroll si le contenu dépasse
                        }}
                    >
                        {history.length > 0 ? (
                            history.map((item) => (
                                <li
                                    key={item.id} // Utiliser l'ID unique comme clé
                                    className="list-group-item mb-2"
                                    style={{
                                        cursor: 'pointer',
                                        borderRadius: '6px',
                                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                                    }}
                                    onDoubleClick={() => handleClassDoubleClick(item)} // Charge la classe au double-clic
                                >
                                    <div>
                                        <strong>Classe :</strong>
                                        <pre className="text-primary">
                        {truncateToThreeLines(item.javaClass)}
                    </pre>
                                    </div>
                                    <div>
                                        <strong>Erreurs / État :</strong>
                                        <p className="text-danger">{truncateToThreeLines(item.error) || 'Aucune'}</p>
                                    </div>
                                    <div>
                                        <strong>Explications :</strong>
                                        <p>{truncateToThreeLines(item.xplanation) || 'Aucune'}</p>
                                    </div>
                                    <div>
                                        <strong>Corrections :</strong>
                                        <p className="text-success">{truncateToThreeLines(item.correction) || 'Aucune'}</p>
                                    </div>

                                </li>
                            ))
                        ) : (
                            <p>Aucune donnée disponible</p>
                        )}
                    </ul>
                </div>
            ) : (
                <div className="App-right bg-dark-subtle p-3">
                    <h2 className= "text-center">Xplain Application</h2>

                    <textarea
                        className="form-control"
                        rows="8"
                        placeholder="Entrez votre classe ici"
                        value={javaClass}
                        onChange={(e) => setJavaClass(e.target.value)}
                    />

                    {errorCompilation === 'SUCCESS' ? (
                        <div className="border border-success rounded p-2 my-3">
                            Compilation réussie !
                        </div>
                    ) : errorCompilation && (
                        <div className="mt-4">
                            <h5 className="text-danger">Erreurs détectées :</h5>
                            <div className="p-3 rounded" style={{backgroundColor: '#f8d7da', color: '#721c24'}}>
                                {errorCompilation}
                            </div>
                        </div>
            )}

            {tokens.length > 0 && (
                <div className="mt-4">
                    <h5 className="text-black">Explications :</h5>
                    <div className="mt-3 border border-success bg-light p-3 rounded">
                        <pre className="code-container">{tokens.join(' ')}</pre>
                    </div>
                </div>
            )}

            {tokensCorrection.length > 0 && (
                <div className="mt-4">
                    <h5 className="text-success">Correction :</h5>
                    <div className="p-3 rounded" style={{backgroundColor: '#d4edda', color: '#155724'}}>
                        <pre>{tokensCorrection.join(' ')}</pre>
                    </div>
                </div>
                    )}

                    <label htmlFor="llm-select" className="mt-3">
                        <h5>Choisissez une version de LLM :</h5>
                    </label>
                    <select
                        id="llm-select"
                        className="form-select mt-2"
                        value={selectedVersion}
                        onChange={(e) => setSelectedVersion(e.target.value)}
                    >
                        <option value="rapide">rapide</option>
                        <option value="moyen">moyen</option>
                        <option value="lent">lent</option>
                    </select>

                    <div className="d-flex gap-2">
                        <div className="button-container">
                            <button
                                onClick={startSSE}
                                className="btn btn-primary d-flex align-items-center gap-2"
                                disabled={isOpen} // Désactivé si l'exécution est en cours
                            >
                                Compilation & Explications
                                {isOpen && !isCorrection ? (
                                    <span className="spinner-border spinner-border-sm" role="status"
                                          aria-hidden="true"></span>
                                ) : (
                                    ""
                                )}

                            </button>
                            <button
                                onClick={startCorrectionSSE}
                                className="btn btn-secondary d-flex align-items-center gap-2"
                                disabled={isOpen || !showCorrectionButton || errorCompilation === "SUCCESS"} // Désactivé si compilation réussie ou bouton non activé
                            >
                                Correction
                                {isCorrection ? (
                                    <span className="spinner-border spinner-border-sm" role="status"
                                          aria-hidden="true"></span>

                                ) : (
                                    ""
                                )}

                            </button>


                        <button onClick={handleBackToHistory} className="btn btn-primary d-flex align-items-center gap-2" disabled={isOpen}>
                            Historique
                        </button>
                        </div>
                    </div>
                    </div>
                    )
                    }

                    {
                !showEditor && (
                    <button
                        onClick={() => setShowEditor(true)}
                        className="btn btn-success btn-sm position-fixed"
                        style={{
                            top: '20px',
                        right: '20px',
                        zIndex: '1000',
                    }}
                >
                    Ajouter une classe
                </button>
            )}
        </div>
    );
}

export default App;
