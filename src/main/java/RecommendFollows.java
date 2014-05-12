/**     William Lyon
 *      CSCI 557 - Machine Learning
 *      Spring 2014
 *      Final Project
 *      Graph Based Link Prediction in a Collaboration Network
 *
 */

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.util.*;

/** RecommendFollows
 *
 */
public class RecommendFollows {
    private GraphDatabaseService graphDB;
    private ExecutionEngine engine;
    private Integer folds;
    private Integer k;
    private Integer predicted_links;
    private Integer leaveout_links;
    private Integer user_count;
    private ArrayList<Integer> users;   // user ids of users to generate recommendations
    private Integer valid_count;        // increment each time generateRecs returns true
    private Integer rec_count;          // total number of users for which recommendations were generated
    private Integer pred_link_count;    // total number of predicated links


    /** Initialize Neo4j graph database connection
     *
     * @param DB_PATH   relative path of Neo4j data store (graph.db)
     */
    public RecommendFollows(String DB_PATH) {
        this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        registerShutdownHook(graphDB);
        this.engine = new ExecutionEngine(graphDB);
        if (graphDB.isAvailable(10)) {
            System.out.println("Database connection established");
        } else {
            System.out.println("ERROR: Unable to establish database connection");
            System.exit(1);
        }
    }

    /** Log result statistics
     *
     */
    public void reportResults() {
        // FIXME: implement AUC(?) and Precision

        // Simple logging for first pass implementation
        System.out.println("*************************************************");
        System.out.println("k: " + this.k);
        System.out.println("Predicted links: " + this.predicted_links);
        System.out.println("Total pred_link_count: " + this.pred_link_count);
        System.out.println("Total rec_count: " + this.rec_count);
        System.out.println("Total valid_count: " + this.valid_count);
        System.out.println("Accuracy: " + (1.0*this.valid_count/this.rec_count));
        System.out.println("Precision: " + (1.0*this.valid_count/this.pred_link_count));
        System.out.println("*************************************************");

    }


    /** Run recommender system with cross fold validation, TODO: define how results are reported
     *
     * @param folds             Number of cross validation folds to run
     * @param k                 Number of neighbors to use for voting
     * @param predicted_links   Number of predicted links to generate
     * @param leaveout_links    Number of test links to leave out for validation
     * @param user_count        Number of users to include in
     */
    public void runWithCrossValidation(Integer folds, Integer k, Integer predicted_links, Integer leaveout_links, Integer user_count) {
        this.folds = folds;
        this.k = k;
        this.predicted_links = predicted_links;
        this.leaveout_links = leaveout_links;
        this.user_count = user_count;

        this.rec_count = 0;
        this.valid_count = 0;
        this.pred_link_count = 0;

        // get list of users ordered by number of FOLLOWS

        for (int v=0; v < folds; v++) {
            this.users = getRandomUsers(user_count);
            Map<String,Object> linkMap = new HashMap<String,Object>();

            for (Integer id : this.users) {
                try {
                    linkMap = recommend(id);
                    if ((Boolean) linkMap.get("test_in_pred")) {
                        this.valid_count += 1;
                    }
                    this.rec_count += 1;
                    this.pred_link_count += ((List) linkMap.get("pred")).size();
                } catch (NullPointerException e) {
                    System.out.println("Null Pointer exception");
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    System.out.println("Exception during recommendation: ");
                    System.out.println(e.getMessage());
                }
            }
        }
        //this.users = getTopUsers(user_count);

        //Map<String,Object> linkMap = new HashMap<String, Object>();

        //for (Integer id : this.users) {
        //    linkMap = recommend(id);
        //    if ((Boolean)linkMap.get("test_in_pred")) {
        //        this.valid_count += 1;
        //    }
        //    this.rec_count += 1;
        //    this.pred_link_count += ((List)linkMap.get("pred")).size();
        //}

    }

    /** Query graph database for user_ids of users with most outgoing :FOLLOWS edges
     *
     * @param num_users             Number of users to return
     * @return ArrayList<Integer>   User ids of top users
     */
    public ArrayList<Integer> getTopUsers(Integer num_users) {

        ArrayList<Integer> resultsArray = new ArrayList<Integer>();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("num_users", num_users);

        String query =
                "MATCH (u:User)-[:FOLLOWS]->(o) WITH u, count(o) as c ORDER BY c DESC RETURN u.id as id, c SKIP 50 LIMIT {num_users}";

        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while(result.hasNext()) {
            Map<String, Object> row = result.next();
            resultsArray.add((Integer)row.get("id"));
        }

        return resultsArray;
    }

    /** Query graph database for user_ids at random
     *
     * @param num_users             Number of users to return
     * @return ArrayList<Integer>   User ids
     */
    public ArrayList<Integer> getRandomUsers(Integer num_users) {
        ArrayList<Integer> resultsArray = new ArrayList<Integer>();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("num_users", num_users);

        String query =
            "MATCH (u1:User)-[:FOLLOWS]->(x) WITH u1, count(x) as num\n" +
            "ORDER BY num DESC\n" +
            "WITH u1 LIMIT 10000\n" +
            "WITH u1, rand() as random\n" +
            "ORDER BY random\n" +
            //"WITH u1 LIMIT 1\n" +
            "RETURN u1.id AS id LIMIT {num_users}";

        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            resultsArray.add((Integer)row.get("id"));
        }

        return resultsArray;
    }

    /** Generate recommendations for a specific user
     *
     * @param user_id
     * @return          Map {test_in_pred, test, pred, u1}
     *                      test_in_pred -> was the test id (the link held-out) in the predicted link set?
     *                      test         -> the link held-out
     *                      pred         -> the set of predicted links
     *                      u1           -> the user_id for which we are generating recommendations
     */
    public Map<String,Object> recommend(Integer user_id) {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_count", user_count);
        params.put("user_id", user_id);
        params.put("leaveout_links", leaveout_links);
        params.put("k", k);
        params.put("predicted_links", predicted_links);


        String query =
                        "// SELECT user at random from top 1000 follows initiated\n" +
                        "// remove one FOLLOWS edge at random\n" +
                        "// finds jaccard for all users with follow intersect\n" +
                        "// select kNN based on jaccard, allow each neighbor to vote (TODO: weight the vote!)\n" +
                        "// take x top recommendations\n" +
                        "// replace remove edge\n" +
                        "// Is test edge in x recommendations?\n" +
                        "MATCH (u1:User {id: {user_id}})-[:FOLLOWS]->(o) WITH o, rand() as r, u1 \n" +
                        "ORDER BY r \n" +
                        "WITH u1, o LIMIT {leaveout_links} \n" +
                        "MATCH (u1)-[r]-(o) DELETE  r WITH o,u1 \n" +
                        "MATCH (u1)-[:FOLLOWS]->(x)<-[:FOLLOWS]-(u2:User) WHERE u1 <> u2 WITH u1, u2,o\n" +
                        "MATCH (u1)-[r:FOLLOWS]->(intersection)<-[:FOLLOWS]-(u2) WITH u1, u2, count(intersection) as intersect, o\n" +
                        "MATCH (u1)-[r:FOLLOWS]->(rest1) WITH u1, u2, intersect, collect(DISTINCT rest1) AS coll1, o\n" +
                        "MATCH (u2)-[r:FOLLOWS]->(rest2) WITH u1, u2, collect(DISTINCT rest2) AS coll2, coll1, intersect, o\n" +
                        "WITH u1, u2, intersect, coll1, coll2, length(coll1 + filter(x IN coll2 WHERE NOT x IN coll1)) as union, o\n" +
                        "WITH o,u1, u2, (1.0*intersect/union) as jaccard ORDER BY jaccard DESC LIMIT {k} \n" +
                        "//CREATE (u1)<-[:Jaccard{coef: (1.0*intersect/union)}]-(u2)\n" +
                        "CREATE UNIQUE (u1)-[:FOLLOWS]->(o) WITH o, u2, u1\n" +
                        "MATCH (u2)-[f:FOLLOWS]->(u3) WITH u1, count(f) as count_f, o.id as test, u3  ORDER BY count_f DESC LIMIT {predicted_links} \n" +
                        "WITH collect(u3.id) as pred, test, u1 \n" +
                        "RETURN (test IN pred) AS test_in_pred, test, pred, u1.id AS id";

        Map<String, Object> resultMap = new HashMap<String, Object>();
        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            resultMap.put("test_in_pred", row.get("test_in_pred"));
            resultMap.put("test", row.get("test"));
            resultMap.put("pred", row.get("pred"));
            resultMap.put("id", row.get("id"));
        }

        System.out.println(resultMap.toString());
        return resultMap;
    }



    /** Main method
     *
     *
     * @param args  Command line arguments - not used
     */
    public static void main(String[] args){
        RecommendFollows rec_sys = new RecommendFollows("data/graph.db");   // Initialize graph DB


        // Run test with cross validation
        // folds=10, k=50, predicted_links=50, leaveout_links=1, user_count=1000
        //rec_sys.runWithCrossValidation(10, 50, 50, 1, 100);
        //rec_sys.runWithCrossValidation(5, , 25, 1, 100);
        //rec_sys.runWithCrossValidation(folds, k, predicted_links, leaveout_links, user_count);

        rec_sys.runWithCrossValidation(10, 5, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 5, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 5, 100, 1, 10);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(10, 10, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 10, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 10, 100, 1, 10);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(10, 25, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 25, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 25, 100, 1, 10);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(10, 50, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 50, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 50, 100, 1, 10);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(10, 100, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 100, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 100, 100, 1, 10);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(10, 1000, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 1000, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 1000, 100, 1, 10);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(10, 2000, 25, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 2000, 50, 1, 10);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(10, 2000, 100, 1, 10);
        rec_sys.reportResults();



        // TODO: report results
        //rec_sys.reportResults();

    }


    /** Register shutdown hook for the Neo4j instance so that it shuts down when the VM exits
     *
     * @param graphDB   The Neo4j instance to shutdown
     */
    private static void registerShutdownHook(final GraphDatabaseService graphDB) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
        public void run(){
                graphDB.shutdown();
            }
        });
    }


}
