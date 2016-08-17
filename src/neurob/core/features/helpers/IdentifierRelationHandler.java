package neurob.core.features.helpers;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class IdentifierRelationHandler {
	private Set<AdjacencyNode> adjacencyList; 
	
	public IdentifierRelationHandler() {
		adjacencyList = new HashSet<AdjacencyNode>();
	}
	
	/**
	 * Add an identifier to the adjacency list if it is not already in
	 * @param id The identifier to be added
	 */
	public void addIdentifier(String id){
		adjacencyList.add(new AdjacencyNode(id));
	}
	
	public int getUniqueIdentifierCount(){
		return adjacencyList.size();
	}
	
	public String getIdentifiers(){
		String s = "";
		
		for(AdjacencyNode n : adjacencyList){
			s += n.getId()+";";
		}
		
		return s;
		
	}
	
	/**
	 * Holds an identifier and a list of identifiers it has a relation to
	 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
	 *
	 */
	private class AdjacencyNode {
		private String id;
		private Set<AdjacencyNode> relatedIds;
		
		public AdjacencyNode(String identifier){
			id = identifier;
			relatedIds = new HashSet<AdjacencyNode>();
		}
		
		public String getId(){ return id;}
		
		@Override
		public boolean equals(Object obj){
			System.out.println("hey");
			if(obj instanceof AdjacencyNode){
				return id.equals(((AdjacencyNode) obj).getId());
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return id.hashCode();
		}
	}

}
