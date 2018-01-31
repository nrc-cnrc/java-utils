package ca.nrc.data.harvesting;

import java.io.IOException;

public interface IPageVisitor {

	public void visitPage(String url, String htmlContent, String plainTextContent) throws PageHarvesterException;
}
