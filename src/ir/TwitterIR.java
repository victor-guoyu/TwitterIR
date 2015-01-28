package ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class TwitterIR {
    private Logger mainLog;

    public Logger getMainLog() {
        return mainLog;
    }

    public void init() {
        Configuration.getInstance();
        mainLog = LogManager.getLogger(TwitterIR.class.getName());
        mainLog.info("Log Started...");

        try {
            mainLog.info("Indexing Twitter Messages......");
            indexTwitterMessages(getIndexWriter());
            mainLog.info("Finished indexing the messages ");
        } catch (IOException e) {
            e.printStackTrace();
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
        EnglishAnalyzer enAnalyzer = new EnglishAnalyzer(stopWordSet);
        File indexPath = new File(Configuration.getInstance().getLuenceIndexPath());
        if (!indexPath.exists()) {
            indexPath.mkdirs();
        }
        Directory indexDir = FSDirectory.open(indexPath);
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
            String[] temp = line.split("\\s+");
            //A field that is indexed but not tokenized: 
            //For example this might be used for a 'country' field or an 'id' field
            doc.add(new StringField("id", temp[0], Field.Store.YES));
            // Indexed, tokenized
            doc.add(new TextField("tweets", temp[1], Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        reader.close();
        indexWriter.close();
    }

    public static void main(String[] args) {
        TwitterIR ir = new TwitterIR();
        ir.init();
    }
}