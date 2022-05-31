package com.example.signtalk.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Sentence {
    @SerializedName("Sentence")
    @Expose
    private String Sentence;

    public String getSentence() {
        return Sentence;
    }

    public void setSentence(String sentence) {
        Sentence = sentence;
    }
}
