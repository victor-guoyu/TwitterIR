package ir;

import ir.QueryReader.QueryProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class TwitterIR {
    private Logger mainLog;
    private EnglishAnalyzer enAnalyzer;
    private QueryParser queryParser;
    private Directory indexDir;
    private final Level RESULT = Level.forName("RESULT", 450);
    private enum TwitterMsg {
        ID, TWEET
    }
    public Logger getMainLog() {
        return mainLog;
    }

    public void init() {
        Configuration.getInstance();
        mainLog = LogManager.getLogger(TwitterIR.class);
        mainLog.info("Log Started...");
        try {
            mainLog.info("Indexing Twitter Messages.....");
            indexTwitterMessages(getIndexWriter());
            mainLog.info("Finished indexing the messages, start searching");
            getStat();
            searchDocs();
            mainLog.info("............DONE.............");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searching through Docs while reading XML Queries
     * @throws Exception 
     */
    private void searchDocs() throws Exception {
        File queryDoc = new File(Configuration.getInstance().getQueriesFilePath());
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(indexDir));
        queryParser = new QueryParser(TwitterMsg.TWEET.name(), enAnalyzer);

        @SuppressWarnings("unused")
        QueryReader queryReader = new QueryReader(queryDoc,
                new QueryProcessor() {

                    @Override
                    public void process(String queryId, String queryString) throws Exception {
                            Query query = queryParser.parse(queryString);
                            TopScoreDocCollector docCollector = TopScoreDocCollector.create(1000, true);
                            searcher.search(query, docCollector);
                            ScoreDoc[] hits = docCollector.topDocs().scoreDocs;
                            logResult(queryId, searcher, hits);
                    }

        });
    }

    /**
     * Helper method to display and log the result
     * @throws IOException 
     */
    private void logResult(String queryId,IndexSearcher searcher, ScoreDoc[] hits) throws IOException {
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = searcher.doc(hits[i].doc);  // getting actual document
            
            String msg = new StringBuilder()
                .append(queryId)
                .append("\tQ0\t")
                .append(hitDoc.get(TwitterMsg.ID.name()))
                .append("\t")
                .append(i+1)
                .append("\t")
                .append(hits[i].score)
                .append("\tmyRun")
                .toString();
            mainLog.info(msg);
            mainLog.log(RESULT, msg);
        }
    }

    /**
     * @return IndexWriter based on the Stop words provided
     */
    private IndexWriter getIndexWriter() throws IOException {
        File stopWordsFile = new File(Configuration.getInstance()
                .getStopWordsFilePath());
        CharArraySet stopWordSet = Files.readLines(stopWordsFile, Charsets.UTF_8,
 
                new LineProcessor<CharArraySet>() {
                    CharArraySet result = new CharArraySet(Configuration
                            .getInstance().getStopWordsSize(), true);

                    @Override
                    public CharArraySet getResult() {
                        return result;
                    }

                    @Override
                    public boolean processLine(String line) throws IOException {
                        if (Strings.isNotBlank(line)) {
                            result.add(line.trim());
                        }
                        return true;
                    }
                });
        enAnalyzer = new EnglishAnalyzer(stopWordSet);
        File indexPath = new File(Configuration.getInstance().getLuenceIndexPath());
        if (!indexPath.exists()) {
            indexPath.mkdirs();
        }
        indexDir = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, enAnalyzer);
        return new IndexWriter(indexDir, config);
    }

    private void indexTwitterMessages(IndexWriter indexWriter) throws IOException {
        BufferedReader reader = new BufferedReader(
                new FileReader(new File(Configuration.getInstance().getTwitterDataFilePath())));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Document doc = new Document();
            String[] temp = line.split("\t");
            //A field that is indexed but not tokenized: 
            //For example this might be used for a 'country' field or an 'id' field
            doc.add(new StringField(TwitterMsg.ID.name(), temp[0], Field.Store.YES));
            // Indexed, tokenized
            doc.add(new TextField(TwitterMsg.TWEET.name(), temp[1], Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        reader.close();
        indexWriter.close();
    }

    private void getStat() throws IOException {
        IndexReader reader = DirectoryReader.open(indexDir);
        FileWriter fw = new FileWriter(new File("logs/tokens.log"));
        BufferedWriter bw = new BufferedWriter(fw);
        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(TwitterMsg.TWEET.name());
        TermsEnum iterator = terms.iterator(null);
        BytesRef byteRef = null;
        while((byteRef = iterator.next()) != null) {
                String term = byteRef.utf8ToString();
                bw.write(term);
                bw.newLine();
        }
        bw.close();
    }

    public static void main(String[] args) {
        TwitterIR ir = new TwitterIR();
        ir.init();
    }
}