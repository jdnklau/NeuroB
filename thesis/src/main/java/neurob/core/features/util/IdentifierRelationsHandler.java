package neurob.core.features.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jannik Dunkelau
 */
public class IdentifierRelationsHandler {
	private AdjacencyList adjacencyList;

	public IdentifierRelationsHandler(){
		adjacencyList = new AdjacencyList();
	}

	/**
	 * Add an identifier to the handler, if it has not already been added
	 * @param id Identifier to add
	 */
	public void addIdentifier(String id){
		adjacencyList.addNode(id);
	}

	/**
	 * Sets two identifiers into relation. If they are not contained in this handler,
	 * adds them beforehand.
	 * @param id1
	 * @param id2
	 */
	public void addIdentifierRelation(String id1, String id2){
		addIdentifier(id1);
		addIdentifier(id2);
		adjacencyList.addEdge(id1, id2);
	}

	/**
	 * Add an edge between two identifiers, if not already present.
	 * Further set id1 as lower bound for id2: id1 < id2
	 * @param id1 Lower bound for id2
	 * @param id2 Upper bound for id1
	 */
	public void addLowerBoundRelation(String id1, String id2){
		addIdentifierRelation(id1, id2);
		adjacencyList.addLowerBoundRelation(id1, id2);
	}

	/**
	 * Add an edge between two identifiers, if not already present.
	 * Further set id1 as upper bound for id2: id1 > id2
	 * @param id1 Upper bound for id2
	 * @param id2 Lower bound for id1
	 */
	public void addUpperBoundRelation(String id1, String id2){
		addIdentifierRelation(id1, id2);
		adjacencyList.addUpperBoundRelation(id1, id2);
	}

	/**
	 * Sets whether a lower or upper bounding exists for the given identifier's domain.
	 * <p>
	 *     This is additive to already existing boundaries.
	 *     If the node already is lower bounded, and {@code addBoundaries(false,true)}
	 *     is called, it remains lower bounded.
	 * </p>
	 * @param setLowerBound true iff the identifier shall be lower bounded
	 * @param setUpperBound true iff the identifier shall be upper bounded
	 */
	public void addDomainBoundaries(String id, boolean setLowerBound, boolean setUpperBound){
		adjacencyList.addDomainBoundaries(id, setLowerBound, setUpperBound);
	}

	public boolean containsId(String id){
		return adjacencyList.containsId(id);
	}

	/**
	 * Checks whether the given identifiers are in relation to each other.
	 * If one of them was not yet added, returns trivially false.
	 * @param id1
	 * @param id2
	 * @return true iff id1 and id2 stand in relation to each other
	 */
	public boolean containsIdRelation(String id1, String id2){
		return adjacencyList.areInRelation(id1, id2);
	}

	/**
	 *
	 * @return Amount of distinct identifiers added to this handler
	 */
	public int getIdCount(){
		return (int) adjacencyList.getIdentiferSet().stream().count();
	}

	/**
	 * Returns the amount of identifies without lower and upper boundaries.
	 * <p>
	 *     This corresponds only to symbolic boundaries, posed between identifiers,
	 *     <b>not</b> to actually bounded domains
	 * </p>
	 * @return Amount of distinct identifiers without lower and upper boundaries
	 */
	public int getUnboundedIdCount(){
		return (int) adjacencyList.getNodeSet().stream().filter(n->n.isUnbounded()).count();
	}

	/**
	 * Returns the amount of identifies with either lower or upper boundaries, but not both.
	 * <p>
	 *     This corresponds only to symbolic boundaries, posed between identifiers,
	 *     <b>not</b> to actually bounded domains
	 * </p>
	 * @return Amount of distinct identifiers with either lower or upper boundaries, but not both
	 */
	public int getSemiBoundedIdCount(){
		return (int) adjacencyList.getNodeSet().stream().filter(n->n.isSemiBounded()).count();
	}

	/**
	 * Returns the amount of identifies with both lower and upper boundaries.
	 * <p>
	 *     This corresponds only to symbolic boundaries, posed between identifiers,
	 *     <b>not</b> to actually bounded domains
	 * </p>
	 * @return Amount of distinct identifiers with both lower and upper boundaries
	 */
	public int getBoundedIdCount(){
		return (int) adjacencyList.getNodeSet().stream().filter(n->n.isBounded()).count();
	}

	/**
	 * @return Amount of identifiers with unbounded domains
	 */
	public int getUnboundedDomainsCount(){
		return (int) adjacencyList.getNodeSet().stream().filter(n->n.hasUnboundedDomain()).count();
	}
	/**
	 * @return Amount of identifiers with either lower or upper bounded domains, not both
	 */
	public int getSemiBoundedDomainsCount(){
		return (int) adjacencyList
				.getNodeSet().stream().filter(n->n.hasSemiBoundedDomain()).count();
	}
	/**
	 * @return Amount of identifiers with both, lower and upper bounded domains
	 */
	public int getBoundedDomainsCount(){
		return (int) adjacencyList.getNodeSet().stream().filter(n->n.hasBoundedDomain()).count();
	}

	/**
	 * @return Amount of relations between identifiers
	 */
	public int getIdRelationsCount(){
		return adjacencyList.getNodeSet().stream().mapToInt(n->n.getRelatedIds().size()).sum() / 2;
		// Note division by two: the upper expression counts each relation twice
		// (once for each identifier of the relation in question)
	}

	public List<String> getIds() {
		return adjacencyList.getIdentiferSet().stream().collect(Collectors.toList());
	}
}
