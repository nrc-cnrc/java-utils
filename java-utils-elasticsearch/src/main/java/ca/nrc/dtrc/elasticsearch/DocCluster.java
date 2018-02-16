package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocCluster {
	
	private Set<String> docIDs = new HashSet<String>();
		public Set<String> getDocIDs() {return docIDs;}
		public void addDocID(String id) {docIDs.add(id);}

	@JsonIgnore
	public int getSize() {
		// TODO Auto-generated method stub
		return getDocIDs().size();
	}

}
