package fr.uge.enskred.application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


/**
 * Représente un graphe non orienté entre des nœuds identifiés par leur clé publique RSA.
 * Ce graphe est utilisé pour déterminer les routes possibles entre nœuds, notamment pour des
 * protocoles de type onion routing.
 *
 * ---
 *
 * Les connexions entre nœuds sont modélisées via une map `network`, tandis que `rootNetwork`
 * contient pour chaque nœud les routes calculées par parcours en largeur (BFS).
 * 
 * Cette classe est thread-safe via l'utilisation d'un verrou `ReentrantLock`.
 */
public final class Graphe {
	
	private static final int NOMBRE_CHEMIN_MAXIMUM = 20; //Limite de chemins explorés pour éviter l'explosion combinatoire
	private static final Logger logger = Logger.getLogger(Graphe.class.getName());
	
	private final HashSet< PublicKeyRSA > keys = new HashSet<>();
	private final HashMap<PublicKeyRSA, HashSet<PublicKeyRSA>> network;
	private final HashMap<PublicKeyRSA, HashMap<PublicKeyRSA, PublicKeyRSA>> rootNetwork;
	
	private final ReentrantLock lock = new ReentrantLock();

    /**
     * Construit un graphe vide.
     */
	Graphe() {
		this.network = new HashMap<>();
		this.rootNetwork = new HashMap<>();
		logger.setLevel(Level.SEVERE);
	}


    /**
     * Retourne la table de routage (parents dans l'arbre BFS) d'un nœud donné.
     *
     * @param key: La clé publique du nœud source.
     * @return La map destination -> parent pour reconstruire le chemin.
     */
	public HashMap<PublicKeyRSA, PublicKeyRSA> get(PublicKeyRSA key) {
		Objects.requireNonNull(key);
		lock.lock();
		try {
			return rootNetwork.get(key);
		} finally {
			lock.unlock();
		}
	}

    /**
     * Met à jour le graphe avec un nouveau réseau.
     * Reconstruit tous les chemins à partir de chaque nœud.
     *
     * @param networkToUpdate: Le nouveau graphe d'adjacence à utiliser.
     */
	public void updateNetWork(HashMap<PublicKeyRSA, HashSet<PublicKeyRSA>> networkToUpdate) {
		Objects.requireNonNull(networkToUpdate);
		lock.lock();
		try {
			network.clear();
			network.putAll(networkToUpdate);
			
			keys.clear();
			keys.addAll(network.keySet());
			network.values().forEach(keys::addAll);
			
			rootNetwork.clear();
			getChemins();
		} finally {
			lock.unlock();
		}
	}
	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------

	private HashMap<PublicKeyRSA, PublicKeyRSA> parcoursLargeur(PublicKeyRSA depart) {
		Objects.requireNonNull(depart);
		lock.lock();
		try {
			var parents = new HashMap<PublicKeyRSA, PublicKeyRSA>();
			var visites = new HashSet<PublicKeyRSA>();
			var a_traiter = new ArrayDeque<PublicKeyRSA>();
			a_traiter.add(depart);
			visites.add(depart);
			parents.put(depart, depart);
			
			while (!a_traiter.isEmpty()) {
				var u = a_traiter.removeFirst();
				var voisins = network.getOrDefault(u, new HashSet<>());
				for (var v : voisins) {
					if (!visites.contains(v)) {
						visites.add(v);
						parents.put(v, u); // u est le prédécesseur de v
						a_traiter.addLast(v);
					}
				}
			}
			return parents;
		} finally {
			lock.unlock();
		}
	}


    /**
     * Calcule pour chaque nœud du graphe les routes vers tous les autres via BFS.
     */
	public void getChemins() {
		lock.lock();
		try {
			for (var depart : keys) {
				var parents = parcoursLargeur(depart);
				//System.out.println(parents);
				rootNetwork.put(depart, parents);
			}			
		} finally {
			lock.unlock();
		}
	}


    /**
     * Calcule le prochain saut (hop) à effectuer depuis un nœud source vers une destination.
     *
     * @param sender:      Le nœud source.
     * @param receiver:    Le nœud destination.
     * @return Le voisin du sender qui mène vers receiver, ou null si aucun chemin.
     */
	public PublicKeyRSA nextHop(PublicKeyRSA sender, PublicKeyRSA receiver) {
		Utils.requireNonNulls(sender, receiver);
		lock.lock();
		try {			
			var routingTable = rootNetwork.get(sender);
			if (routingTable == null) return null;
			
			if (!routingTable.containsKey(receiver)) return null;
			
			if (routingTable.get(receiver).equals(sender) || sender.equals(receiver)) {
				return receiver;
			}
			
			var current = receiver;
			while(!routingTable.get(current).equals(sender)) {
				current = routingTable.get(current);
				System.out.println("Ça boucle en while NextHop");
			}
			return current;
		} finally {
			lock.unlock();
		}
	}

	
    /**
     * Recherche jusqu'à N chemins simples (sans cycle) entre deux nœuds.
     *
     * @param graph: Le graphe sous forme de liste d'adjacence.
     * @param src:   La source.
     * @param dst:   La destination.
     * @return Une liste de chemins (chacun une liste de nœuds).
     */
	public List<List<PublicKeyRSA>> findNPaths(Map<PublicKeyRSA, List<PublicKeyRSA>> graph, PublicKeyRSA src, PublicKeyRSA dst) {
		Utils.requireNonNulls(graph, src, dst);
		lock.lock();
		try {
			int nombreAretes = keys.size() - 1; // Un graphe connexe à n sommet possède n-1 aretes
			if(nombreAretes > NOMBRE_CHEMIN_MAXIMUM) {
				nombreAretes = NOMBRE_CHEMIN_MAXIMUM;
			}
			List<List<PublicKeyRSA>> result = new ArrayList<>();
			LinkedList<PublicKeyRSA> path = new LinkedList<>();
			Set<PublicKeyRSA> visited = new HashSet<>();
			dfs(graph, src, dst, nombreAretes, result, path, visited);
			return result;
		} finally {
			lock.unlock();
		}
    }

    private void dfs(Map<PublicKeyRSA, List<PublicKeyRSA>> graph, PublicKeyRSA current, PublicKeyRSA dst, int n,
                            List<List<PublicKeyRSA>> result, LinkedList<PublicKeyRSA> path, Set<PublicKeyRSA> visited) {
    	Utils.requireNonNulls(graph, current, dst, result, path, visited);
		if (result.size() >= n) return;
		
		visited.add(current);
		path.add(current);
		
		if (current.equals(dst)) {
			result.add(new ArrayList<>(path));
		} else {
			for (PublicKeyRSA neighbor : graph.getOrDefault(current, Collections.emptyList())) {
				if (!visited.contains(neighbor)) {
					dfs(graph, neighbor, dst, n, result, path, visited);
				}
			}
		}
		
		path.removeLast();
		visited.remove(current);
		
    }
    
    private int generateRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * Retourne un chemin aléatoire entre deux nœuds.
     * Le chemin est choisi parmi ceux trouvés par findNPaths.
     *
     * @param sender:       Le nœud source.
     * @param destinataire: Le nœud destination.
     * @return Un chemin aléatoire entre sender et destinataire.
     */
    public List<PublicKeyRSA> randomPath(PublicKeyRSA sender, PublicKeyRSA destinataire) {
    	Utils.requireNonNulls(sender, destinataire);
        lock.lock();
        try {
            // Crée une version List de chaque entrée du graphe
            Map<PublicKeyRSA, List<PublicKeyRSA>> graphListMap = new HashMap<>();
            for (var entry : network.entrySet()) {
                graphListMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            var allPaths = findNPaths(graphListMap, sender, destinataire);
            if (allPaths.isEmpty()) return Collections.emptyList();
            for(var path: allPaths) {
            	path.forEach(y -> logger.info(y + " -> "));
            	logger.info("");
            }
            int randomIndex = generateRandomInt(allPaths.size());
            return new ArrayList<>(allPaths.get(randomIndex));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Représentation textuelle complète du graphe (nœuds, connexions et routage).
     *
     * @return Une chaîne représentant l'état du graphe.
     */
	@Override
	public String toString() {
		lock.lock();
		try {	
			var builder = new StringBuilder();
			builder.append("Graphe:\n");
			
			builder.append("Keys:\n");
			for(var key : keys) {
				builder.append("  ").append(key).append("\n");
			}
			
			builder.append("Network:\n");
			for(var entry : network.entrySet()) {
				builder.append("  ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
			}
			
			builder.append("Root Network:\n");
			for(var entry : rootNetwork.entrySet()) {
				builder.append("  From ").append(entry.getKey()).append(":\n");
				for(var route : entry.getValue().entrySet()) {
					builder.append("    To ").append(route.getKey()).append(" via ").append(route.getValue()).append("\n");
				}
			}
			
			return builder.toString();
		} finally {
			lock.unlock();
		}
	}

	
//    public static void main(PublicKeyRSA[] args) {
//        Graphe graphe = new Graphe();
//
//        // Création d'un graphe temporaire
//        HashMap<PublicKeyRSA, HashSet<PublicKeyRSA>> tempNetwork = new HashMap<>();
//
//        tempNetwork.put("A", new HashSet<>(List.of("F", "C", "E")));
//        tempNetwork.put("D", new HashSet<>(List.of("B", "C", "E")));
//        tempNetwork.put("F", new HashSet<>(List.of("B", "A")));
//        tempNetwork.put("C", new HashSet<>(List.of("B", "A", "D")));
//        tempNetwork.put("B", new HashSet<>(List.of("F", "C", "D")));
//        tempNetwork.put("E", new HashSet<>(List.of("A", "C", "D")));
//
//        // Met à jour le graphe
//        graphe.updateNetWork(tempNetwork);
//
//        // Affiche les chemins de chaque nœud vers les autres
//        System.out.println("=== Chemins de chaque nœud ===");
//        for (var start : graphe.getRootNetwork().keySet()) {
//            System.out.println("Depuis " + start + ":");
//            for (var target : graphe.getRootNetwork().get(start).keySet()) {
//                PublicKeyRSA parent = graphe.getRootNetwork().get(start).get(target);
//                System.out.println("  -> " + target + " via " + parent);
//            }
//        }
//
//        // Test de nextHop
//        System.out.println("\n=== Test des nextHop ===");
//        PublicKeyRSA[][] tests = {
//            {"A", "D"},
//            {"D", "A"},
//            {"A", "F"},
//            {"F", "D"},
//            {"E", "C"},
//            {"C", "E"},
//            {"B", "B"}
//        };
//
//        for (PublicKeyRSA[] test : tests) {
//            PublicKeyRSA from = test[0];
//            PublicKeyRSA to = test[1];
//            PublicKeyRSA hop = graphe.nextHop(from, to);
//            System.out.println("nextHop(" + from + " -> " + to + ") = " + hop);
//        }
//
//        // Recherche de chemins
//        System.out.println("\n=== Recherche des 3 premiers chemins simples de A à D ===");
//
//        Map<PublicKeyRSA, List<PublicKeyRSA>> graphListMap = new HashMap<>();
//        for (var entry : tempNetwork.entrySet()) {
//            graphListMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
//        }
//
//        List<List<PublicKeyRSA>> paths = graphe.findNPaths(graphListMap, "A", "D");
//
//        int i = 1;
//        for (List<PublicKeyRSA> path : paths) {
//            System.out.println("Chemin " + i++ + ": " + path);
//        }
//
//        // Chemin aléatoire
//        System.out.println("\n=== Chemin aléatoire de A à D ===");
//        for(int h = 0; h < 10; h++)
//        	System.out.println(graphe.randomPath("A", "D"));
//    }



}
