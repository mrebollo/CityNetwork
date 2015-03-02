package main;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import mendeley.MendeleyAPI;
import utils.CSVWriter;
import utils.Log;
import utils.RISWriter;
import utils.SortUtils;
import utils.Zipper;
import utils.CSVReader;
import cortext.CortextAPI;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class Main {

	
	/**
	 * One iteration of request and extraction parts of the algo, given query.
	 * 
	 * @param searchQuery
	 * @param filePref
	 */
	public static void iteration(String searchQuery,String filePref){

		// setup mendeley
		Log.output("Setting up Mendeley...");
		MendeleyAPI.setupAPI();
		
		// construct 100 references from catalog request
		Log.output("Catalog request : "+searchQuery);
		MendeleyAPI.catalogRequest(searchQuery,100);
		Log.output(Reference.references.keySet().size()+" refs in table");
		
		//export them to ris and zip
		Log.output("Writing to ris and zipping...");
		RISWriter.write(filePref+".ris", Reference.references.keySet());
		Zipper.zip(filePref+".ris");
		
		//Cortext
		Log.newLine(1);
		Log.output("Setting up Cortext");
		CortextAPI.setupAPI();
		CortextAPI.deleteAllCorpuses();
		//upload corpus and get keywords
		CortextAPI.getKeywords(CortextAPI.extractKeywords(CortextAPI.parseCorpus(CortextAPI.uploadCorpus(filePref+".zip"))),filePref+"_keywords.csv");
		
	}
	
	
	
	public static void run(String initialQuery,String resFold,int numIteration,int kwLimit){
		//log to file
		Log.initLog("/Users/Juste/Documents/ComplexSystems/CityNetwork/Models/Biblio/AlgoSR/AlgoSRJavaApp/log");
		
		//initial query
		String query = initialQuery;
		
		// run data
		String[][] keywords = new String[numIteration][kwLimit];
		int[] numRefs = new int[numIteration];
		int[][] occs = new int[numIteration][kwLimit];
		
		
		for(int t=0;t<numIteration;t++){
			//get query and extract keywords
			Log.newLine(1);Log.output("Iteration "+t);Log.output("===================");
			
			int currentRefNumber = Reference.references.size();
			iteration(query,resFold+"/refs_"+t);
			if(Reference.references.size()==currentRefNumber){
				Log.output("Convergence criteria : no new ref reached - "+Reference.references.size()+" refs.");
				Log.output("Stopping algorithm");
				break;
			}
			
			//read kw from file, construct new query
			String[][] kwFile = CSVReader.read(resFold+"/refs_"+t+"_keywords.csv","\t");
			
			
			
			//sort kw on occurences, keep most frequent.
			double[] cValues = new double[kwFile.length-1];
			String[] stems = new String[kwFile.length-1];
			for(int i=1;i<kwFile.length;i++){cValues[i-1]=Double.parseDouble(kwFile[i][7].replace(",", "."));stems[i-1]=kwFile[i][0].replace(" ", "+");}
			int[] perm = SortUtils.sortDesc(cValues);
			//for(int i=0;i<perm.length;i++){System.out.print(cValues[perm[i]]+" ; ");}System.out.println();//DEBUG sorting
			
			//construct new request
			query="";
			
			for(int k=0;k<kwLimit;k++){
				//System.out.println(cValues[perm[k]]);
				//System.out.println(stems[perm[k]]);
				String sep = "";
				if(k>0){sep="+";}
				query=query+sep+stems[perm[k]];
				keywords[t][k] = stems[perm[k]];
				occs[t][k] =  (int)cValues[perm[k]];
			}
			
			Log.output("New query is : "+query);
			
			//memorize stats
			// num of refs ; num kws ; C-values (of all ?)
			numRefs[t] = Reference.references.size();

			
		}
		
		//write stats to result file
		String[][] stats = new String[numIteration][2*kwLimit+1];
		for(int t=0;t<numIteration;t++){
			stats[t][0]=new Integer(numRefs[t]).toString();
			for(int k=1;k<kwLimit+1;k++){stats[t][k]=keywords[t][k-1];stats[t][k+kwLimit]=new Integer(occs[t][k-1]).toString();}
		}
		CSVWriter.write(resFold+"/stats.csv", stats, ";");
	}
	
	
	
	/**
	 * @param args
	 *   * no args : query and folder provided in function
	 *   * args.length == 2 : args[0] = query ; args[1] = folder ;
	 *      args[2] = num iterations ; args[3] = kw limit
	 *   
	 * 
	 */
	public static void main(String[] args) throws Exception {
		
		/**
		 * First Results and Tests on algo :
		 *     - many duplicates, have to work more precisely on hashcode for Reference class : OK. (increases complexity but still ok)
		 *     
		 * 	   - stationarity is really unstable ? if a keyword is dominant in a large set of refs, will converge very rapidly ?
		 * 			--> TODO study sensitivity to initial query.
		 * 
		 * 	   - TODO : sensitivity to request constraint ? --> requires a scholar API, not yet.
		 * 
		 * 	   - 
		 */
		String folder="",query="";
		int numIterations = 0,kwLimit=0;
		if(args.length==0){
			// for tests : store results in runs folder in results.
			folder="/Users/Juste/Documents/ComplexSystems/CityNetwork/Results/Biblio/AlgoSR/runs/run_0";
			query = "urban+geography+transportation+planning";
			numIterations = 10;kwLimit = 5;
		}
		else if(args.length==4){query = args[0];folder=args[1];numIterations = Integer.parseInt(args[2]);kwLimit = Integer.parseInt(args[3]);}
		else{throw new Exception("Error : not enough args.");}
		
		try{
		   //run("city+development+transportation+network","data/testRun",10,10);			
			//create dir if does not exists
			(new File(folder)).mkdir();
			run(query,folder,numIterations,kwLimit);
		}catch(Exception e){e.printStackTrace();Log.exception(e.getStackTrace());}
	}

}
