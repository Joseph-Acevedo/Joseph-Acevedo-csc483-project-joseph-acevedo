package edu.arizona.cs;

import edu.stanford.nlp.simple.Sentence;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WikiParser {

    private String inputDirectory;
    private StandardAnalyzer analyzer;
    private Directory index;
    private boolean indexExists;

    public WikiParser(String inputDirectory, boolean indexExists) {
        this.inputDirectory = inputDirectory;
        this.indexExists = indexExists;
    }

    public void buildIndex() throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.index = FSDirectory.open(Paths.get(String.format("%s\\index", this.inputDirectory)));
        if (indexExists)
            return;
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(index, config);

        File directory = new File(this.inputDirectory);

        Document currDoc = null;
        for (File file : directory.listFiles()) {
            // Directory should only contain the txt files
            if (file.isDirectory()) {
                continue;
            }

            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
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
                    // for now treat the same as normal text
                    Sentence titleSent = new Sentence(line);
                    for (String lemma : titleSent.lemmas()) {
                        currDoc.add(new TextField("section", lemma, Field.Store.YES));
                    }
                } else {
                    // line in wiki page
                    // using Stanford CoreNLP for lemmatization
                    try {
                        Sentence standSent = new Sentence(line);
                        for (String lemma : standSent.lemmas()) {
                            currDoc.add(new TextField("text", lemma, Field.Store.YES));
                        }
                    } catch (Exception e) {
                        System.out.println("Error on line: " + line);
                        e.printStackTrace();
                    }
                }
            }
            fileScanner.close();
        }
        writer.close();
        this.indexExists = true;
    }

    public List<ResultClass> answerQuestion(String query, String[] fields) throws IOException {
        Query q = null;
        Sentence querySentence = new Sentence(query);
        try {
            q = new MultiFieldQueryParser(fields, analyzer).parse(QueryParser.escape(String.join(" ", querySentence.lemmas())));
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
            results.add(result);
        }
        return results;
    }

}
