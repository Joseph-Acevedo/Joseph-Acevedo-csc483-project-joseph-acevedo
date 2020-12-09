package edu.arizona.cs;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class WikiParser {

    private static final String STOP_WORDS_FILEPATH = "C:\\Users\\Joseph\\git\\csc483-project-joseph-acevedo\\src\\main\\resources\\stopwords.txt";
    private String inputDirectory;
    private PorterStemmer stemmer;
    private Analyzer analyzer;
    private Directory index;
    private boolean indexExists;
    private static final boolean STOP_WORDS = true;
    private static final boolean LEMMATIZE = true;
    private static final boolean POS_BOOST = true;
    public static float BOOST_FACTOR = 1.1f;

    public WikiParser(String inputDirectory, boolean indexExists) {
        this.inputDirectory = inputDirectory;
        this.indexExists = indexExists;
    }

    public void buildIndex() throws IOException {
        long timeStart = System.currentTimeMillis();
        this.stemmer = new PorterStemmer();
        this.analyzer = new StandardAnalyzer();//StopAnalyzer(Paths.get(STOP_WORDS_FILEPATH));
        if (LEMMATIZE) {
            if (STOP_WORDS)
                this.index = FSDirectory.open(Paths.get(String.format("%s\\cnlp_sw_index", this.inputDirectory)));
            else
                this.index = FSDirectory.open(Paths.get(String.format("%s\\cnlp_nsw_index", this.inputDirectory)));
        } else {
            if (STOP_WORDS)
                this.index = FSDirectory.open(Paths.get(String.format("%s\\ptrstm_sw_index", this.inputDirectory)));
            else
                this.index = FSDirectory.open(Paths.get(String.format("%s\\ptrstm_nsw_index", this.inputDirectory)));
        }
        if (indexExists)
            return;
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(index, config);

        File directory = new File(this.inputDirectory);

        Document currDoc = null;
        for (File file : directory.listFiles()) {
            // Directory should only contain the txt files
            System.out.println("File: " + file.getName());
            if (file.isDirectory()) {
                continue;
            }

            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().toLowerCase();
                line = line.replaceAll("&nbsp;", " ");
                if (line.isEmpty() || line.trim().isEmpty())
                    continue;

                if (line.startsWith("[[") && line.endsWith("]]")) {
                    // [[wiki title]]
                    line = line.substring(2, line.length() - 2);
                    if (line.startsWith("File:"))
                        continue;

                    if (currDoc != null) {
                        writer.addDocument(currDoc);
                    }
                    currDoc = new Document();
                    currDoc.add(new StringField("title", line, Field.Store.YES));
                } else if (line.startsWith("==") && line.endsWith("==")) {
                    // ==section title==
                    if (LEMMATIZE) {
                        addLemmaToDoc(currDoc, "section", line);
                    } else {
                        addStemToDoc(currDoc, "section", line);
                    }
                } else {
                    // line in wiki page
                    if (LEMMATIZE) {
                        addLemmaToDoc(currDoc, "text", line);
                    } else {
                        addStemToDoc(currDoc, "text", line);
                    }
                }
            }
            fileScanner.close();
        }
        writer.close();
        this.indexExists = true;
        System.out.println("Building collection took: " + (System.currentTimeMillis() - timeStart)/1000/60 + " minutes");
    }

    private void addLemmaToDoc(Document doc, String key, String sentence) {
        try {
            Sentence sent = new Sentence(sentence);
            for (String lemma : sent.lemmas()) {
                doc.add(new TextField(key, lemma, Field.Store.YES));
            }
        } catch (Exception e) {
            System.out.println("Error on line: " + sentence);
            e.printStackTrace();
        }
    }

    private void addStemToDoc(Document doc, String key, String sentence) {
        try {
            String[] tokens = sentence.split("[\\.,\\s!;?:\"]+");
            for (String token : tokens) {
                stemmer.setCurrent(token);
                stemmer.stem();
                doc.add(new TextField(key, stemmer.getCurrent(), Field.Store.YES));
            }
        } catch (Exception e) {
            System.out.println("Error on line: " + sentence);
            e.printStackTrace();
        }
    }

    public List<ResultClass> answerQuestion(String query) throws IOException {
        Query q = null;
        try {
            List<String> tags = getPropNouns(query);
            query = query.toLowerCase();

            if (LEMMATIZE) {
                Sentence sent = new Sentence(query);
                query = String.join(" ", sent.lemmas());
            } else {
                String[] tokens = query.split("[\\.,\\s!;?:\"]+");
                query = "";
                for (String token : tokens) {
                    stemmer.setCurrent(token);
                    stemmer.stem();
                    query += stemmer.getCurrent() + " ";
                }
            }

            query = QueryParser.escape(query);
            if (POS_BOOST) {
                int prevTermIdx = 0;
                for (String noun : tags) {
                    int idx = query.indexOf(noun, prevTermIdx);
                    if (idx == -1) {
                        continue;
                    }
                    prevTermIdx = idx + 1;
                    query = query.substring(0, idx + noun.length()) + "^" + BOOST_FACTOR + query.substring(idx + noun.length());
                }
            }

            q = new QueryParser("text", analyzer).parse(query);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        // Change similarity here
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        List<ResultClass> results = new ArrayList();
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);

            ResultClass result = new ResultClass(d, hits[i].score);
            // System.out.printf("%d. [%s](%.5f)\n", i+1, d.get("title"), hits[i].score);
            results.add(result);
        }
        return results;
    }

    private List<String> getPropNouns(String line) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument doc = pipeline.processToCoreDocument(line);
        List<String> tags = new ArrayList();
        for (CoreLabel tok : doc.tokens()) {
            if (tok.tag().equals("NNP") || tok.tag().equals("NNPS")) {
                tags.add(tok.word().toLowerCase());
            }
        }
        return tags;
    }
}
