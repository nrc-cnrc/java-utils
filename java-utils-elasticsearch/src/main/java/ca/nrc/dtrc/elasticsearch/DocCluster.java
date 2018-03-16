package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocCluster {
	
	private Set<String> docIDs = new HashSet<String>();
		public Set<String> getDocIDs() {return docIDs;}
		public void addDocID(String id) {docIDs.add(id);}

	public void addDocIDs(Collection<String> docIDs) {
		for (String id: docIDs) {
			addDocID(id);
		}
	}

		
	private String lang = "en";
		public void setLang(String _lang) {this.lang = _lang;}
		public String getLang() {return this.lang;}
		
	private Double cohesion;
		public void setCohesion(Double _cohesion) {this.cohesion = _cohesion;}
		public Double getCohesion() {return this.cohesion;}

	@JsonIgnore
	public Integer getSize() {
		return getDocIDs().size();
	}
	
}
