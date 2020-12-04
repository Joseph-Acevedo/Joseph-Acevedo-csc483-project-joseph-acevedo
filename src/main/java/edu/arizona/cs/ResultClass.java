package edu.arizona.cs;

import org.apache.lucene.document.Document;

public class ResultClass {

    public Document doc;
    public float score;

    public ResultClass(Document doc, float score) {
        this.doc = doc;
        this.score = score;
    }
}
