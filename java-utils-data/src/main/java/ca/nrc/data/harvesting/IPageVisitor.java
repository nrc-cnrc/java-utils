package ca.nrc.data.harvesting;

public interface IPageVisitor {

	public void visitPage(String url, String htmlContent, String plainTextContent) throws PageHarvesterException;
}
