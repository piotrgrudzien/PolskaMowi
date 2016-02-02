
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SparseInstance;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;


public class Processing {
	static List<String> stopWords = new ArrayList<String>();
	/*
	 * Mapping between keywords and the number of documents they occur in
	 */
	static Map<String, Integer> preIndexed = new HashMap<String, Integer>();
	/*
	 * List of all (or specified number) of keywords that will be used to create vectors of data
	 */
	static List<String> indexedDictionary = new ArrayList<String>();
	/*
	 * List of filtered lines split into arrays of keywords
	 */
	static List<String[]> filteredLines = new ArrayList<String[]>();
	/*
	 * Number of lines to be read from the file
	 */
	static int limit = 1000;
	/*
	 * Number of keywords to be included in the dictionary (length of data vector)
	 */
	static int dictionarySize = 300;
	/*
	 * Defining K, the number of clusters
	 */
	static int K = 2;
	/*
	 * Number of top keywords displayed for each cluster
	 */
	static int keywordsDisplayed = 8;
	/*
	 * Static data set
	 */
	static Dataset data = new DefaultDataset();
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		initialiseStopWords();
		BufferedReader br = new BufferedReader(new FileReader("D:\\eclipse\\Workspace\\Thinking in Java\\5gazetaPolishfile.txt"));
		String line;
		InputStream is = new FileInputStream("D:\\eclipse\\opennlp\\en-token.bin");
		
		TokenizerModel model = new TokenizerModel(is);
		Tokenizer tokenizer = new TokenizerME(model);
		int i = 0;
		

		
		while((line = br.readLine()) != null && i < limit){
			String noPol = removePol(line);
			String[] rawLine = tokenizer.tokenize(noPol);
			String[] filtLine = filter(rawLine);
			if(filtLine.length < 11){
				continue;
			}
			filteredLines.add(filtLine);
			i++;
			if(i % 1000 == 0){
				System.out.println(i + " lines have been read");
			}
		}
		System.out.println("Read in and filtered " + i + " lines");
		long duration = System.currentTimeMillis() - startTime;
		System.out.println("With building filtered tokenized lines took " + duration/1000 + " s");
		System.out.println("Or " + duration/60000 + " min");
		for(String[] filteredLine : filteredLines){
			//System.out.println(Arrays.toString(filteredLine));
			//To calculate the number of documents the word occurred in, not the total number of occurrences
			for(int a = 0; a < filteredLine.length; a++){
				boolean firstOccurrence = true;
				for(int b = 0; b < a; b++){
					firstOccurrence = filteredLine[a] != filteredLine[b];
					if(!firstOccurrence){
						break;
					}
				}
				if(!firstOccurrence){
					continue;
				}
				/*
				 * Put all words in a map which keeps track of in how many documents a words has occurred
				 */
				if(preIndexed.containsKey(filteredLine[a])){
					preIndexed.put(filteredLine[a], preIndexed.get(filteredLine[a]) + 1);
				}
				else{
					preIndexed.put(filteredLine[a], 1);
				}
			}
		}
		/*
		 * Take a certain number (dictionarySize) of most popular (occurring in the biggest number of documents) keywords
		 * and put them in the indexedDictionary which is a list of keywords in the order of popularity which will be used
		 * to build data vectors.
		 */
				int prevMaxKey = -1;
				dictionarySize = preIndexed.size();
				for(int ii = 0; ii < dictionarySize; ii++){
					int maxKey = -1;
					String currentKey = "";
					for (Map.Entry<String, Integer> entry : preIndexed.entrySet())
					{
					    if(entry.getValue() > maxKey && (entry.getValue() <= prevMaxKey || prevMaxKey == -1)){
					    	if(entry.getValue() == prevMaxKey){
					    		if(indexedDictionary.contains(entry.getKey())){
					    			continue;
					    		}
					    	}
					    	maxKey = entry.getValue();
					    	currentKey = entry.getKey();
					    }
					}
					prevMaxKey = maxKey;
					indexedDictionary.add(currentKey);
					//System.out.println("Adding " + currentKey + " which occured in " + maxKey + " documents");
				}
		duration = System.currentTimeMillis() - startTime;
		System.out.println("With indexing took " + duration/1000 + " s");
		System.out.println("Or " + duration/60000 + " min");
		System.out.println("Indexed Dictionary contains " + indexedDictionary.size() + " entries");
		System.out.println("Which is a reduced preIndexed Dictionary whose size is " + preIndexed.size());
		/*
		 * Build data vector for every line and insert it into the dataset.
		 * In the meantime build reference mapping 
		 */
		for(String[] filteredLine : filteredLines){
			double[] vectorisedLine = toDoubleArray(filteredLine);
			//System.out.println(Arrays.toString(vectorisedLine));
			if(vectorisedLine != null){	
				Instance instance = new SparseInstance(vectorisedLine.length);
				for(int index = 0; index < vectorisedLine.length; index++){
					if(vectorisedLine[index] != 0){
						instance.put(index, vectorisedLine[index]);
					}
				}
				data.add(instance);
			}
		}
		duration = System.currentTimeMillis() - startTime;
		System.out.println("With building dataset and reference set took " + duration/1000 + " s");
		System.out.println("Or " + duration/60000 + " min");
		System.out.println("Dataset and reference set built");
		is.close();
		
		Clusterer clusterer = new KMeans(K);
		System.out.println("Clustering of " + data.size() + " elements into " + K + " clusters has started");
		Dataset[] result = clusterer.cluster(data);
		duration = System.currentTimeMillis() - startTime;
		System.out.println("With clustering took " + duration/1000 + " s");
		System.out.println("Or " + duration/60000 + " min");
		System.out.println("Clustering results: ");
		for(Dataset cluster : result){
			System.out.println();
			System.out.println("Cluster contains the following " + cluster.size() + " elements: ");
			System.out.println("Top keywords: " + Arrays.toString(mainKey(cluster)));
		}
		System.out.println("SUCCESS");
		duration = System.currentTimeMillis() - startTime;
		System.out.println("Altogether took " + duration/1000 + " s");
		System.out.println("Or " + duration/60000 + " min");
	}

	private static String[] mainKey(Dataset cluster) {
		double[] avg = new double[indexedDictionary.size()];
		for(int i = 0; i < indexedDictionary.size(); i++){
			avg[i] = 0.0;
		}
		for(Instance instance : cluster){
			for(int i = 0; i < indexedDictionary.size(); i++){
				avg[i] += instance.value(i);
			}
		}
		for(int i = 0; i < indexedDictionary.size(); i++){
			avg[i] /= indexedDictionary.size();
		}
		//System.out.println("Cluster vector: " + Arrays.toString(avg));
		String[] words = new String[keywordsDisplayed];
		for(int i = 0; i < keywordsDisplayed; i++){
			double max = 0.0;
			int maxIndex = -1;
			for(int j = 0; j < indexedDictionary.size(); j++){
				if(avg[j] > max){
					max = avg[j];
					maxIndex = j;
				}
			}
			System.out.println("Max index = " + maxIndex);
			if(maxIndex != -1){
				words[i] = indexedDictionary.get(maxIndex);
			}
			else{
				words[i] = "ERROR";
			}
			System.out.println("Word = " + words[i]);
			if(maxIndex != -1){
				avg[maxIndex] = 0;
			}
		}
		return words;
	}

	private static double[] toDoubleArray(String[] filtLine) {
		double[] vector = new double[indexedDictionary.size()];
		for(int i = 0; i < vector.length; i++){
			vector[i] = 0.0;
		}
		double length = 0.0;
		for (String word : filtLine){
			int index = indexedDictionary.indexOf(word);
			if(index == -1)
			{
				continue;
			}
			vector[index]++;
			length++;
		}
		if(length == 0){
			return null;
		}
		length = Math.sqrt(length);
		for(int i = 0; i < vector.length; i++){
			//vector[i] *= 100/length;
			//vector[i] at this moment is equal to tf (how many times the word occured in the current document
			//so multiply it by log(N/number of documents it occurred in)
			/*
			if(i < 5){
			System.out.println(indexedDictionary.get(i) + ", tf = " + vector[i] + ", N = " + filteredLines.size()
					+ ", if = " + preIndexed.get(indexedDictionary.get(i)) + 
					", idf = " + Math.log(filteredLines.size()/preIndexed.get(indexedDictionary.get(i))));
			System.out.println("tf-idf = " + vector[i]*Math.log(filteredLines.size()/preIndexed.get(indexedDictionary.get(i))));
			vector[i] *= Math.log(filteredLines.size()/preIndexed.get(indexedDictionary.get(i)));
			}*/
			if(Double.isNaN(vector[i])){
				return null;
			}
		}
		return vector;
	}

	private static String removePol(String line) {
		Character[] Polish = {'¹', 'ê', 'ó', 'œ', '³', '¿', 'Ÿ', 'æ', 'ñ'};
		Character[] nonPolish = {'a', 'e', 'o', 's', 'l', 'z', 'z', 'c', 'n'};
		for(int i = 0; i < Polish.length; i++){
			line = line.replace(Polish[i], nonPolish[i]);
		}
		return line;
	}

	private static String[] filter(String[] rawLine) {
		List<String> list = new ArrayList<String>();
		for(int i = 0; i < rawLine.length; i++){
			if((!isStopWord(rawLine[i])) && rawLine[i].matches("^[a-zA-Z0-9#]*$") && rawLine[i].length() > 1 &&
					!rawLine[i].matches("^[0-9]*$")){
				list.add(rawLine[i]);
			}
		}
		
		return list.toArray(new String[list.size()]);
	}

	private static boolean isStopWord(String word) {
		for(String entry : stopWords){
			if(word.equalsIgnoreCase(entry)){
				return true;
			}
		}
		return false;
	}

	private static void initialiseStopWords() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("D:\\eclipse\\Java ml\\polishStopWords.txt"));
		String line;
		while((line = br.readLine()) != null){
			stopWords.add(line);
		}
	}
}
