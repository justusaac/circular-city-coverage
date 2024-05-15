
import java.util.List;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Stack;
import java.util.function.BooleanSupplier;

public class DominatingSet{
	public List<BitSet> adjacency;
	public DominatingSet(List<BitSet> adjacency){
		this.adjacency = adjacency;
	}
	
	List<List<Integer>> disjoint_subsets(){
		List<List<Integer>> subsets = new ArrayList<>();
		BitSet found = new BitSet();
		Stack<Integer> stack = new Stack<>();
		for(int i=0; i<adjacency.size(); i++){
			if(found.get(i)){
				continue;
			}
			List<Integer> subset = new ArrayList<>();
			found.set(i);
			stack.push(i);
			while(!stack.empty()){
				int curr = stack.pop();
				subset.add(curr);
				for(int j=adjacency.get(curr).length(); (j=adjacency.get(curr).previousSetBit(j-1))>=0;){
					if(!found.get(j)){
						stack.push(j);
						found.set(j);
					}	
				}
			}
			subsets.add(subset);
		}
		return subsets;
	}

	List<BitSet> subset_adjacency(List<Integer> subset){
		List<BitSet> output = new ArrayList<>();
		for(int i=0; i<subset.size(); i++){
			output.add(new BitSet());
			output.get(i).set(i);
			for(int j=0; j<i; j++){
				boolean adj = adjacency.get(subset.get(i)).get(subset.get(j));
				output.get(i).set(j, adj);
				output.get(j).set(i, adj);
			}
		}
		return output;
	}

	protected static class SearchState{
		public List<BitSet> adjacency;
		public BitSet candidates;
		public BitSet selected;
		public BitSet need;
		public SearchState(List<BitSet> adjacency){
			this.adjacency = adjacency;
			this.candidates = new BitSet(adjacency.size());
			this.candidates.set(0,adjacency.size());
			this.selected = new BitSet(adjacency.size());
			this.need = new BitSet(adjacency.size());
			this.need.set(0,adjacency.size());
		}
		public SearchState(SearchState other){
			this.adjacency = other.adjacency;
			this.candidates = (BitSet)other.candidates.clone();
			this.selected = (BitSet)other.selected.clone();
			this.need = (BitSet)other.need.clone();
		}

		public boolean is_complete(){
			return this.need.isEmpty();
		}
		public void select(int choice){
			this.candidates.set(choice, false);
			this.selected.set(choice);
			this.need.andNot(adjacency.get(choice));
		}

		public void reduce(){
			List<BooleanSupplier> reduction_steps = new ArrayList<>();
			reduction_steps.add(() -> remove_subsets());
			reduction_steps.add(() -> select_required());
			reduction_steps.add(() -> discard_subsumed());
			reduction_steps.add(() -> counting_selection());

			//Running every reduction step until all of them fail in a row
			//You could run them in a cycle or run them in a predetermined order that resets on a success
			for(int i=0,false_streak=0; false_streak<reduction_steps.size(); i=(i+1)%reduction_steps.size()){
				false_streak = reduction_steps.get(i).getAsBoolean() ? 0 : false_streak+1;
			}
			/*
			for(int i=0; i<reduction_steps.size();){
				i = (reduction_steps.get(i).getAsBoolean() && i>0) ? 0 : i+1;
			}*/
		}

		//No need to select a node whose additions are a subset of another node's
		protected boolean remove_subsets(){
			boolean removed = false;
			for(int i=candidates.length(); (i=candidates.previousSetBit(i-1))>=0;){
				for(int j=candidates.length(); (j=candidates.previousSetBit(j-1))>i;){
					if(additions_subset(i,j)){
						this.candidates.set(i, false);
						removed = true;
						break;
					}
					if(additions_subset(j,i)){
						this.candidates.set(j, false);
						removed = true;
					}
				}
			}
			return removed;
		}
		//Are there no uncovered nodes adjacent to i but not j?
		protected boolean additions_subset(int i, int j){
			BitSet additions = (BitSet)this.need.clone();
			additions.and(adjacency.get(i));
			additions.andNot(adjacency.get(j));
			return additions.isEmpty();
		}

		//If there's only one candidate that selects a node, it must be selected
		protected boolean select_required(){
			boolean added = false;
			for(int i=need.length(); (i=need.previousSetBit(i-1))>=0;){
				BitSet all = (BitSet)adjacency.get(i).clone();
				all.and(candidates);
				int first = all.nextSetBit(0);
				if(first==all.length()-1){
					this.select(first);
					added = true;
				}
			}
			return added;
		}

		//If every time a node is covered another uncovered node is also covered, we can ignore it
		protected boolean discard_subsumed(){
			boolean removed = false;
			for(int i=need.length(); (i=need.previousSetBit(i-1))>=0;){
				for(int j=need.length(); (j=need.previousSetBit(j-1))>i;){
					if(ways_subset(j,i)){
						this.need.set(i, false);
						removed = true;
						break;
					}
					if(ways_subset(i,j)){
						this.need.set(j, false);
						removed = true;
					}
				}
			}
			return removed;
		}
		//Are there no candidates adjacent to i but not j?
		protected boolean ways_subset(int i, int j){
			BitSet ways = (BitSet)this.candidates.clone();
			ways.and(adjacency.get(i));
			ways.andNot(adjacency.get(j));
			return ways.isEmpty();
		}

		/*
		Considering all the elements that appear in a particular set S and exactly one other candidate set
		Take the union of the other sets that these elements are in and remove anything in S
		If this set is smaller than the number of elements in S of frequency 2, then select S
		*/
		protected boolean counting_selection(){
			boolean added = false;
			//Assemble all frequency 2 elements
			BitSet two_ways = new BitSet(adjacency.size());
			for(int i=need.length(); (i=need.previousSetBit(i-1))>=0;){
				BitSet ways = (BitSet)adjacency.get(i).clone();
				ways.and(candidates);
				//2nd set bit equal to last set bit <-> cardinality=2
				if(ways.nextSetBit(ways.nextSetBit(0)+1)+1 == ways.length()){
					two_ways.set(i);
				}
			}
			for(int i=candidates.length(); (i=candidates.previousSetBit(i-1))>=0;){
				BitSet additions = (BitSet)adjacency.get(i).clone();
				additions.and(two_ways);
				if(additions.cardinality()==0){
					continue;
				}
				//Assemble the contents of the other sets that the frequency 2 elements are in 
				BitSet other_additions = new BitSet(adjacency.size());
				for(int j=additions.length(); (j=additions.previousSetBit(j-1))>=0;){
					BitSet ways = (BitSet)adjacency.get(j).clone();
					ways.and(candidates);
					int other_way = ways.length()-1!=i ? ways.length()-1 : ways.nextSetBit(0);
					other_additions.or(adjacency.get(other_way));
				}
				other_additions.andNot(adjacency.get(i));
				if(other_additions.cardinality() < additions.cardinality()){
					added = true;
					this.select(i);
				}
			}
			return added;
		}

	}

	public BitSet solve(){
		List<List<Integer>> subsets = disjoint_subsets();
		BitSet answer = new BitSet(adjacency.size());
		for(int i=0; i<subsets.size(); i++){
			BitSet subset_answer = solve_section(subset_adjacency(subsets.get(i)));
			for(int j=subset_answer.length(); (j=subset_answer.previousSetBit(j-1))>=0;){
				answer.set(subsets.get(i).get(j));
			}
		}
		return answer;
	}

	BitSet solve_section(List<BitSet> adjacency){
		SearchState init = new SearchState(adjacency);
		init.reduce();
		if(init.is_complete()){
			return init.selected;
		}
		Stack<SearchState> stack = new Stack<>();
		//Iterative deepening depth first search
		for(int limit=init.selected.cardinality()+1; limit<=adjacency.size(); limit++){
			stack.push(init);
			while(!stack.empty()){
				SearchState curr = stack.pop();
				for(int j=curr.candidates.length(); (j=curr.candidates.previousSetBit(j-1))>=0;){
					SearchState nxt = new SearchState(curr);
					nxt.select(j);
					if(nxt.selected.cardinality()>=limit){
						if(nxt.is_complete()){
							return nxt.selected;
						}
						continue;
					}
					nxt.reduce();
					if(nxt.selected.cardinality()>=limit){
						if(nxt.selected.cardinality()==limit && nxt.is_complete()){
							return nxt.selected;
						}
						continue;
					}
					stack.push(nxt);
				}
			}
			//Increase depth limit and try again
		}
		return new BitSet();
	}

}
