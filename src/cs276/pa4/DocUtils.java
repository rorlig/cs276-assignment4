package cs276.pa4;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by guptaga on 5/29/17.
 */
public class DocUtils {

    static String[] TFTYPES = {"url","title","body","header","anchor"};
    static double urlweight = 0.3;
    static double titleweight  = 0.6;
    static double bodyweight = 0.3;
    static double headerweight = 0.8;
    static double anchorweight = 0.2;
    static double smoothingBodyLength = 1.0;


    public static double[] getInstance(Document d, Query q, Map<String, Double> idfs, Map<String, Map<String, Double>> trainRels){
        Map<String,Map<String, Double>> docMap = getDocTermFreqs(d,q);
        Map<String,Double> queryMap = getQueryFreqs(q,idfs);
        double[] vector = new double[TFTYPES.length + 1];
        for(int i = 0; i < TFTYPES.length; i++){
            String key = TFTYPES[i];
            Map<String, Double> docVec = docMap.get(key);



            double score = dotProduct(queryMap, docVec);
            vector[i] = score;
        }
        if(trainRels != null){
            Map<String,Double> urlScores = trainRels.get(q.query);
            double rel = urlScores.get(d.url);
            vector[TFTYPES.length] = rel;
        }else{
            vector[TFTYPES.length] = 0;
        }
        return vector;
    }

    //Add word to Map
    private static void addKeyNIncrement(String key, Map<String,Double> tf, int inc){
        if(tf.containsKey(key)){
            tf.put(key, new Double(tf.get(key) + inc));
        }else{
            tf.put(key, new Double(inc));
        }
    }
    //Given a url string and a query word, tokenize on non alphaneumeric characters
    //and count the times the query word appears
    private static void urlTF(String qWord, String[] url, Map<String,Double> tf){
        for(String domain: url){
            if(qWord.equals(domain)){
                addKeyNIncrement(domain, tf, 1);
            }
        }
    }
    //Given a title string, tokenize on whitespace
    //and count the times the query word appears
    private static void titleTF(String qWord, String[] title, Map<String,Double> tf){
        if(title == null) return;
        for(String h: title){
            if(qWord.equals(h)){
                addKeyNIncrement(h, tf, 1);
            }
        }

    }
    //Loop through the headers list, tokenize the strings on whitespace
    //and count the times the query word appears
    private static void headerTF(String qWord, List<String> headers, Map<String,Double> tf){
        if(headers == null) return;
        for(String head: headers){
            String[] splits = head.split("\\s+");
            for(String split: splits){
                if(qWord.equals(split)){
                    addKeyNIncrement(split, tf, 1);
                }
            }
        }
    }
    //Loop through the body_hits key set and if the key is equal to a query word,
    //get the size of that keys list
    private static void body_hitsTF(String qWord, Map<String,List<Integer>> body_hits, Map<String,Double> tf){
        if(body_hits == null) return;
        for (Map.Entry<String, List<Integer>> entry : body_hits.entrySet()){
            String key = entry.getKey();
            if(qWord.equals(key)){
                //tf.put(key, new Double(entry.getValue().size()));
                addKeyNIncrement(key, tf, entry.getValue().size());
            }
        }
    }
    //Loop the anchors key_set and tokenize the key. If the key is equal to the query word,
    //add that keys value.
    private static void anchorsTF(String qWord, Map<String, Integer> anchors, Map<String,Double> tf){
        if(anchors == null) return;
        for (Map.Entry<String, Integer> entry : anchors.entrySet()){
            String key = entry.getKey();
            String[] links = key.split("\\s+");
            HashSet<String> mySet = new HashSet<String>(Arrays.asList(links));
            if(mySet.contains(qWord)){
                addKeyNIncrement(qWord, tf, entry.getValue());
            }
        }
    }

    /*/
	 * Creates the various kinds of term frequencies (url, title, body, header, and anchor)
	 * You can override this if you'd like, but it's likely that your concrete classes will share this implementation.
	 */
//    private static Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {
//        // Map from tf type -> queryWord -> score
//        Map<String,Map<String, Double>> tfs = new HashMap<String,Map<String, Double>>();
//        Map<String,Double> url = new HashMap<String,Double>();
//        Map<String,Double> title = new HashMap<String,Double>();
//        Map<String,Double> body = new HashMap<String,Double>();
//        Map<String,Double> header = new HashMap<String,Double>();
//        Map<String,Double> anchor = new HashMap<String,Double>();
//        tfs.put(TFTYPES[0], url);
//        tfs.put(TFTYPES[1], title);
//        tfs.put(TFTYPES[2], body);
//        tfs.put(TFTYPES[3], header);
//        tfs.put(TFTYPES[4], anchor);
//        // Loop through query terms and increase relevant tfs. Note: you should do this to each type of term frequencies.
//        HashSet<String> hs = new HashSet<String>(q.queryWords);
//        String[] urlSplits = (d.url.toLowerCase()).split("\\W+");
//        String[] titleSplits = null;
//        if(d.title != null) titleSplits = (d.title).split("\\s+");
//        for (String queryWord: hs) {
//            urlTF(queryWord,urlSplits,url);
//            titleTF(queryWord,titleSplits,title);
//            headerTF(queryWord,d.headers,header);
//            body_hitsTF(queryWord,d.body_hits,body);
//            anchorsTF(queryWord,d.anchors,anchor);
//        }
////        normalizeDoc(tfs);
//        return tfs;
//    }



    //copied from assignment 3...
    /**
     * Accumulate the various kinds of term frequencies
     * for the fields (url, title, body, header, and anchor).
     * You can override this if you'd like, but it's likely
     * that your concrete classes will share this implementation.
     * @param d the Document
     * @param q the Query
     */
    public static Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {

        // Map from tf type -> queryWord -> score
        Map<String,Map<String, Double>> tfs = new HashMap<>();
        for(String str:TFTYPES)
            tfs.put(str, new HashMap<String, Double>());

        //query words are wrapped around a set for uniqueness
        for (String queryWord : q.queryWords) {

            try {
                //url
                if (d.url!=null) {
                    URL u = new URL(d.url);
                    HashSet<String> hset = new HashSet<>();
                    hset.addAll(Arrays.asList(u.getHost().split(".")));
                    hset.addAll(Arrays.asList(u.getPath().split("/")));
                    for (String s : hset) {
                        if (s.indexOf('.')!=-1){
                            s=s.split("\\.")[0];

                        }
                        if (s.equals(queryWord))
                            if (tfs.get("url").containsKey(queryWord))
                                tfs.get("url").put(queryWord, tfs.get("url").get(queryWord) + 1);
                            else
                                tfs.get("url").put(queryWord, 1D);
                    }
                }
                //title
                if (d.title!=null) {
                    for (String s : d.title.split(" ")) {
                        if (s.equals(queryWord))
                            if (tfs.get("title").containsKey(queryWord))
                                tfs.get("title").put(queryWord, tfs.get("title").get(queryWord) + 1);
                            else
                                tfs.get("title").put(queryWord, 1D);
                    }
                }
                //headers
                if (d.headers!=null) {
                    HashSet<String> headerWords = new HashSet<String>();
                    for (String header : d.headers)
                        headerWords.addAll(Arrays.asList(header.split(" ")));
                    for (String s : headerWords) {
                        if (s.equals(queryWord))
                            if (tfs.get("header").containsKey(queryWord))
                                tfs.get("header").put(queryWord, tfs.get("header").get(queryWord) + 1);
                            else
                                tfs.get("header").put(queryWord, 1D);
                    }
                }
                //bodyhits
                if (d.body_hits!=null) {
                    for (String key : d.body_hits.keySet()) {
                        if (key.equals(queryWord))
                            if (tfs.get("body").containsKey(queryWord))
                                tfs.get("body").put(queryWord, tfs.get("body").get(queryWord) + d.body_hits.get(key).size());
                            else
                                tfs.get("body").put(queryWord, (double) d.body_hits.get(key).size());
                    }
                }
                //anchors
                if (d.anchors!=null) {
                    for (String atext : d.anchors.keySet()) {
                        for (String s : atext.split(" ")) {
                            if (s.equals(queryWord))
                                if (tfs.get("anchor").containsKey(queryWord))
                                    tfs.get("anchor").put(queryWord, tfs.get("anchor").get(queryWord) +anchorweight * d.anchors.get(atext));
                                else
                                    tfs.get("anchor").put(queryWord, anchorweight * (double) d.anchors.get(atext));
                        }
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        }
        normalizeDoc(tfs);
        return tfs;
    }


//    private static Map<String,Double> getQueryFreqs(Query q, Map<String, Double> idfs){
//        Map<String,Double> query = new HashMap<String,Double>(1);
//        List<String> words = q.queryWords;
//        HashSet<String> seen = new HashSet<String>();
//        for(String word : words){
//            if(!seen.contains(word)){
//                if(idfs.containsKey(word)){
//                    query.put(word,idfs.get(word));
//                }else{
//                    query.put(word,1.0);
//                }
//            }
//        }
//        return query;
//    }
    /**
     * Get frequencies for a query.
     * @param q the query to compute frequencies for
     * Copied from AScorer...
     */
    public static Map<String,Double> getQueryFreqs(Query q, Map<String, Double> idfs) {


        // queryWord -> term frequency
        Map<String,Double> tfQuery = new HashMap<String, Double>();

        //weight function - wf = 1+log(tf) if tf>0 else 0; wf'=wf*idf;
        List<String> tokens=q.queryWords;
        HashSet<String> uniqTokens=new HashSet<>();
        uniqTokens.addAll(tokens);
        for(String s:uniqTokens){
            tfQuery.put(s,idfs.getOrDefault(s,1D)
                    * (1+Math.log(getCount(s,tokens))));
        }
        return tfQuery;
    }


    private static int getCount(String str, List<String> list){
        int count=0;
        if (!list.contains(str))
            return count;
        for(String s:list)
            if (s.equals(str))
                count++;
        return count;
    }

    private ArrayList<Double> vectorOperation(String operation, ArrayList<Double> list1,ArrayList<Double> list2){
        ArrayList<Double> result=new ArrayList<Double>();
        if (operation.equals("add")){
            if (list1.size()==0)
                return list2;
            else {
                if (list1.size()!=list2.size())
                    return null;
                else {
                    for (int i = 0; i < list1.size(); i++) {
                        result.add(i, list1.get(i) + list2.get(i));
                    }
                    return result;
                }

            }
        } else {
            //this is for multiply
            if (list1.size()!=list2.size()){
                return null;
            } else {
                for (int i=0;i<list1.size();i++){
                    result.add(i,list1.get(i)*list2.get(i));
                }
                return result;
            }
        }
    }

    private static double dotProduct(Map<String,Double> a, Map<String,Double> b){
        double score = 0.0;
        for (Map.Entry<String, Double> entry : a.entrySet()){
            String key = entry.getKey();
            if(b.containsKey(key)){
                score = score + entry.getValue()*b.get(key);
            }
        }
        return score;
    }


    //Need to count the terms in each dimension and return it squared.
    private static double squaredDimensionCount(Map<String, Double> termFreqs) {
        double count = 0;
        for(Map.Entry<String,Double> entry : termFreqs.entrySet()){
            count = count + entry.getValue().doubleValue();
        }
        return count*count;
    }

    //Normalizes a vector by dividing each tf score by the magnitude
    private static void normalizeDoc(Map<String,Map<String,Double>> termFreqs){
        double sumDistSquared = 0.0;
        for(Map.Entry<String, Map<String, Double>> entry: termFreqs.entrySet()){
            Map<String,Double> raw = entry.getValue();
            sumDistSquared = sumDistSquared + squaredDimensionCount(raw);
        }
        double magnitude = Math.sqrt(sumDistSquared);
        for(Map.Entry<String, Map<String, Double>> entry: termFreqs.entrySet()){
            Map<String,Double> raw = entry.getValue();
            for(Map.Entry<String, Double> entry2: raw.entrySet()){
                raw.put(entry2.getKey(), entry2.getValue()/magnitude);
            }
        }
    }



}
