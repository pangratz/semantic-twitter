package at.jku.semantic.twitter;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class TwitterQuery {

	public static void main(String[] args) {
		Model model = ModelFactory.createDefaultModel();
		Model jqueryModel = FileManager.get().loadModel("userModels/jquery.xml");
		model.add(jqueryModel);

		StringBuilder queryStr = new StringBuilder();
		queryStr.append("PREFIX foaf: <").append(Constants.FOAF_NS).append("> ");
		queryStr.append("PREFIX stweeter: <").append(Constants.SEMANTIC_TWEETER_NS).append("> ");
		queryStr.append("PREFIX stweet: <").append(Constants.SEMANTIC_TWEET_NS).append("> ");
		queryStr.append("SELECT ?name ?count ");
		queryStr.append("WHERE { ");
		queryStr.append(" ?x foaf:nick ?name . ");
		queryStr.append(" ?x stweeter:statusCount ?count . ");
		queryStr.append("}");

		QueryExecution query = QueryExecutionFactory.create(queryStr.toString(), model);
		ResultSet rs = query.execSelect();
		while (rs.hasNext()) {
			QuerySolution querySolution = rs.nextSolution();
			Literal countLit = querySolution.getLiteral("count");
			Literal nameLit = querySolution.getLiteral("name");
			System.out.println(nameLit.getString() + " has " + countLit.getString() + " tweets");
		}
		query.close();
	}

}
