package de.derivo.sparqldlapi.examples;

import de.derivo.sparqldlapi.Query;
import de.derivo.sparqldlapi.QueryEngine;
import de.derivo.sparqldlapi.QueryResult;
import de.derivo.sparqldlapi.exceptions.QueryEngineException;
import de.derivo.sparqldlapi.exceptions.QueryParserException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.jdom.output.XMLOutputter;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 *
 * @author christian
 */
public class Consult {
    
    private static QueryEngine engine;
    
    /**
     *
     * @param args
     */
    public static void main(String[] args){   
    	if (args.length < 3) {
    		System.out.println("Need three arguments: "
    				+ "Input OWL ontology, output JSON file, a SPARQL-DL query string");
    		System.exit(0);
    	}
        try {
            // Create an ontology manager
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            // Load ontology from Ontology File (owl 2 document)
            File in = new File(args[0]);            
            File out = new File(args[1]);
            out.createNewFile();

            OWLOntology ont = manager.loadOntologyFromOntologyDocument(in);                   
                       
            // Create an instance of an OWL API reasoner
            // Hermit!
            ReasonerFactory factory = new ReasonerFactory();
            OWLReasoner reasoner = factory.createReasoner(ont);
  
            // Optionally let the reasoner compute the most relevant inferences in advance
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.DISJOINT_CLASSES, 
                    InferenceType.OBJECT_PROPERTY_HIERARCHY, InferenceType.CLASS_ASSERTIONS,InferenceType.OBJECT_PROPERTY_ASSERTIONS);
                
            // Create an instance of the SPARQL-DL query engine
            engine = QueryEngine.create(manager, reasoner, true);          
            
            processQuery(args[2], args[1]);
     
        }
        catch(UnsupportedOperationException exception) {
            System.out.println("Unsupported reasoner operation.");
        }
        catch(OWLOntologyCreationException e) {
            System.out.println("Could not load the ontology: " + e.getMessage());
        }        
        catch (IOException e){
            System.out.println("Could not open output file: " + e.getMessage());
        }
    }
    
    public static void processQuery(String q, String outfile) throws IOException{
        try {
                long startTime = System.currentTimeMillis();
                
                // Create a query object from it's string representation
                Query query = Query.create(q);

                System.out.println("\nQuery:");
                System.out.println(q);
                System.out.println("-------------------------------------------------");

                // Execute the query and generate the result set
                QueryResult result = engine.execute(query);

                // print as XML
                try {
                        XMLOutputter outxml = new XMLOutputter();
                        outxml.output(result.toXML(), System.out);
                } 
                catch(IOException e) {
                        // ok, this should not happen
                }

                System.out.println("\nResults:");
                System.out.println("-------------------------------------------------");


                // print as JSON
                System.out.print(result.toJSON());

                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(outfile,true))){
                        writer.append("queryresults:");
                        writer.write(result.toJSON());
                }
                catch(IOException e) {
                        // ok, this should not happen
                        System.out.println("Exception while writing output file:" + e.getMessage());
                }

                System.out.println("-------------------------------------------------");
                System.out.println("Size of result set: " + result.size());
                System.out.println("Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + "s\n");
        }
        catch(QueryParserException e) {
        	System.out.println("Query parser error: " + e);
        }
        catch(QueryEngineException e) {
        	System.out.println("Query engine error: " + e);
        }
    }
	
}
