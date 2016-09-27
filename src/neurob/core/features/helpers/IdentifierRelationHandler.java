package neurob.core.features.helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jannik Dunkelau
 *
 */
public class IdentifierRelationHandler {
	private HashMap<String,AdjacencyNode> adjacencyList; 
	
	public IdentifierRelationHandler() {
		adjacencyList = new HashMap<String,AdjacencyNode>();
	}
	
	/**
	 * Add an identifier to the adjacency list if it is not already in
	 * @param id The identifier to be added
	 */
	public AdjacencyNode addIdentifier(String id){
		AdjacencyNode n;
		String tid = id.trim();
		if(!adjacencyList.containsKey(tid)){
			n = new AdjacencyNode(tid);
			adjacencyList.put(tid, n);
		} else {
			n = adjacencyList.get(tid);
		}
		return n;
	}
	public AdjacencyNode accessNode(String id){
		// syntactic sugar, as using add to access an identifier safely would be counterintuitive
		return addIdentifier(id);
	}
	
	public void addBoundariesToIdentifier(String id, boolean hasLowerBound, boolean hasUpperBound){
		// get ID
		AdjacencyNode n = accessNode(id);
		n.addBoundaries(hasLowerBound,hasUpperBound);
	}
	
	public void identifierUpperDomainRestrictionByIdentifier(String id, String restrictingId){
		AdjacencyNode n = accessNode(id);
		AdjacencyNode m = accessNode(restrictingId);
		
		n.setUpperBound(m);
	}
	
	public void identifierLowerDomainRestrictionByIdentifier(String id, String restrictingId){
		AdjacencyNode n = accessNode(id);
		AdjacencyNode m = accessNode(restrictingId);
		
		n.setLowerBound(m);
	}
	
	
	public int getUniqueIdentifierCount(){
		return adjacencyList.size();
	}
	
	public int getInfiniteDomainIdentifiersCount(){
		int i = 0;
		
		for(String id : adjacencyList.keySet()){
			AdjacencyNode n = adjacencyList.get(id);
			if(n.hasBoundariesSet){
				if(!n.hasLowerBound || !n.hasUpperBound){
					++i;
				}
			}
		}
		
		return i;
	}
	
	public int getFiniteDomainIdentifiersCount(){
		int i = 0;
		
		for(String id : adjacencyList.keySet()){
			AdjacencyNode n = adjacencyList.get(id);
			if(n.hasBoundariesSet()){
				if(n.hasLowerBound() && n.hasUpperBound()){
					++i;
				}
			}
		}
		
		return i;
	}

	public int getUnknownDomainSizeIdentifiersCount(){
		int i = 0;
		
		for(String id : adjacencyList.keySet()){
			AdjacencyNode n = adjacencyList.get(id);
			if(!n.hasBoundariesSet()){
				++i;
			}
		}
		
		return i;
	}
	
	public String getIdentifiers(){
		String s = "";
		
		for(String n : adjacencyList.keySet()){
			s += n+";";
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
		private Set<AdjacencyNode> lowerBoundaries;
		private Set<AdjacencyNode> upperBoundaries;
		private boolean hasBoundariesSet;
		private boolean hasLowerBound;
		private boolean hasUpperBound;
		
		public AdjacencyNode(String identifier){
			id = identifier;
			relatedIds = new HashSet<AdjacencyNode>();
			
			lowerBoundaries = new HashSet<AdjacencyNode>();
			upperBoundaries = new HashSet<AdjacencyNode>();
			
			hasBoundariesSet=false;
			hasLowerBound = false;
			hasUpperBound = false;
		}
		
		public void addBoundaries(boolean hasLowerBound, boolean hasUpperBound) {
			hasBoundariesSet = true;
			this.hasLowerBound = this.hasLowerBound || hasLowerBound;
			this.hasUpperBound = this.hasUpperBound || hasUpperBound;
		}
		
		public void setLowerBound(AdjacencyNode node){
			lowerBoundaries.add(node);
		}
		public void setUpperBound(AdjacencyNode node){
			upperBoundaries.add(node);
		}

		public String getId(){ return id;}
		public boolean hasBoundariesSet(){return hasBoundariesSet;}
		public boolean hasLowerBound(){return hasLowerBound;}
		public boolean hasUpperBound(){return hasUpperBound;}
		
		@Override
		public boolean equals(Object obj){
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
