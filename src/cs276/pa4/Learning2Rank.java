package cs276.pa4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.core.Instances;

public class Learning2Rank {


	public static Classifier train(String train_data_file, String train_rel_file, int task, Map<String,Double> idfs) {
	    System.err.println("## Training with feature_file =" + train_data_file + ", rel_file = " + train_rel_file + " ... \n");
	    Classifier model = null;
	    Learner learner = null;

 		if (task == 1) {
			learner = new PointwiseLearner();
		} else if (task == 2) {
		  boolean isLinearKernel = true;
		  //boolean isLinearKernel = false;
			learner = new PairwiseLearner(isLinearKernel);
		} else if (task == 3) {
//			learner = new AdvancedLearner();
			/*
			 * @TODO: Your code here, add more features
			 * */
			System.err.println("Task 3");

		} else if (task == 4) {

			/*
			 * @TODO: Your code here, extra credit
			 * */
			System.err.println("Extra credit");

		}

		/* Step (1): construct your feature matrix here */
		Instances data = learner.extractTrainFeatures(train_data_file, train_rel_file, idfs);

		/* Step (2): implement your learning algorithm here */
		model = learner.training(data);

	  return model;
	}

	 public static Map<Query, List<Document>> test(String test_data_file, Classifier model, int task, Map<String,Double> idfs){
		 	System.err.println("## Testing with feature_file=" + test_data_file + " ... \n");
		    Map<Query, List<Document>> ranked_queries = new HashMap<>();
		    Learner learner = null;
	 		if (task == 1) {
				learner = new PointwiseLearner();
			} else if (task == 2) {
			  boolean isLinearKernel = true;
			  //boolean isLinearKernel = false;
				learner = new PairwiseLearner(isLinearKernel);
			} else if (task == 3) {
//				learner = new AdvancedLearner();

				/*
				 * @TODO: Your code here, add more features
				 * */
				System.err.println("Task 3");

			} else if (task == 4) {

				/*
				 * @TODO: Your code here, extra credit
				 * */
				System.err.println("Extra credit");

			}

	 		/* Step (1): construct your test feature matrix here */
	 		TestFeatures tf = learner.extractTestFeatures(test_data_file, idfs);

	 		/* Step (2): implement your prediction and ranking code here */
			ranked_queries = learner.testing(tf, model);

		    return ranked_queries;
	 }



	/* This function output the ranking results in expected format */
	public static void writeRankedResultsToFile(Map<Query,List<Document>> queryRankings, PrintStream ps) {
		System.out.println("writing results to file");
		for (Query query : queryRankings.keySet()) {
			StringBuilder queryBuilder = new StringBuilder();
			for (String s : query.queryWords) {
				queryBuilder.append(s);
				queryBuilder.append(" ");
			}

			String queryStr = "query: " + queryBuilder.toString() + "\n";
			ps.print(queryStr);

			for (Document res : queryRankings.get(query)) {
				String urlString =
						"  url: " + res.url + "\n" +
								"    title: " + res.title + "\n" +
								"    debug: "  + "\n";
				ps.print(urlString);
			}
		}
	}


	public static void main(String[] args) throws IOException {
		if (args.length != 5 && args.length != 6) {
			System.err.println("Input arguments: " + Arrays.toString(args));
			System.err.println("Usage: <train_signal_file> <train_rel_file> <test_signal_file> <idfs_file> <task> [ranked_out_file]");
			System.err.println("  ranked_out_file (optional): output results are written into the specified file. If not, output to stdout.");
			return;
		}
		String train_signal_file = args[0];
		String train_rel_file = args[1];
		String test_signal_file = args[2];
		String dfFile = args[3];
		int task = Integer.parseInt(args[4]);
		String ranked_out_file = "";
		if (args.length == 6){
			ranked_out_file = args[5];
		}
//		System.out.println(new File("./idfs").getAbsoluteFile());


	    /* Populate idfs */
//	    String dfFile = "idfs";
	    Map<String,Double> idfs = null;
		idfs = Util.loadDFs(dfFile);

	    /* Train & test */
	    System.err.println("### Running task" + task + "...");
	    Classifier model = train(train_signal_file, train_rel_file, task, idfs);

	    /* performance on the training data */
	    Map<Query, List<Document>> trained_ranked_queries = test(train_signal_file, model, task, idfs);
	    String trainOutFile="tmp.train.ranked";
	    writeRankedResultsToFile(trained_ranked_queries, new PrintStream(new FileOutputStream(trainOutFile)));
	    NdcgMain ndcg = new NdcgMain(train_rel_file);
	    System.err.println("# Trained NDCG=" + ndcg.score(trainOutFile));
//	    (new File(trainOutFile)).delete();

	    Map<Query, List<Document>> ranked_queries = test(test_signal_file, model, task, idfs);

	    /* Output results */
	    if(ranked_out_file.equals("")){ /* output to stdout */
	      writeRankedResultsToFile(ranked_queries, System.out);
	    } else { 						/* output to file */
			writeRankedResultsToFile(ranked_queries, new PrintStream(new FileOutputStream(ranked_out_file)));
		}
	}
}
