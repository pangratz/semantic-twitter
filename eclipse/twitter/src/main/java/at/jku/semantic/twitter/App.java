package at.jku.semantic.twitter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.http.AccessToken;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String args[]) throws Exception {
		Properties props = new Properties();
		props.load(App.class.getResourceAsStream("/twitter.properties"));
		String consumerKey = props.getProperty("twitter.oauth.consumerKey");
		String consumerSecret = props.getProperty("twitter.oauth.consumerSecret");
		String token = props.getProperty("twitter.oauth.token");
		String tokenSecret = props.getProperty("twitter.oauth.tokenSecret");

		AccessToken accessToken = new AccessToken(token, tokenSecret);
		Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);

		OntModel generalModel = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);

		// iterate over each friend and create a RDF model for each one
		IDs friendsIDs = twitter.getFriendsIDs();
		int[] iDs = friendsIDs.getIDs();
		int count = 0;
		for (int id : iDs) {

			User user = twitter.showUser(id);

			Model userModel = ModelFactory.createDefaultModel();

			String foafNS = "http://xmlns.com/foaf/0.1/Person#";
			userModel.setNsPrefix("foaf", foafNS);

			String stNS = "http://twitter.com/semantic/Tweeter#";
			userModel.setNsPrefix("st", stNS);

			// NICKNAME
			Resource usedIdRes = ResourceFactory.createResource(foafNS + user.getId());
			Property nickProp = ResourceFactory.createProperty(foafNS, "nick");
			Literal nickLit = ResourceFactory.createTypedLiteral(user.getScreenName());
			userModel.add(usedIdRes, nickProp, nickLit);

			// STATUS COUNT
			Property statusCountProp = ResourceFactory.createProperty(stNS, "statusCount");
			Literal statusCountLit = ResourceFactory.createTypedLiteral(user.getStatusesCount());
			userModel.add(usedIdRes, statusCountProp, statusCountLit);

			// LOCATION
			Property locationProp = ResourceFactory.createProperty(stNS, "location");
			Literal locationLit = ResourceFactory.createTypedLiteral(user.getLocation());
			userModel.add(usedIdRes, locationProp, locationLit);

			generalModel.add(userModel);

			File userFile = new File("userModels", user.getScreenName() + ".xml");
			userModel.write(new FileOutputStream(userFile));

			count++;

			if (count >= 5)
				break;
		}

		StringBuilder queryStr = new StringBuilder();
		queryStr.append("PREFIX foaf: <http://xmlns.com/foaf/0.1/Person#> ");
		queryStr.append("PREFIX st: <http://twitter.com/semantic/Tweeter#> ");
		queryStr.append("SELECT ?name ?count ");
		queryStr.append("WHERE { ");
		queryStr.append(" ?x foaf:nick ?name .");
		queryStr.append(" ?x st:statusCount ?count");
		queryStr.append("}");

		QueryExecution query = QueryExecutionFactory.create(queryStr.toString(), generalModel);
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
