package com.example.healthapp;

public class DataClass {
    private final String Temperature;
    private final String bpm;
    private final String pressure;

    public DataClass(String temp, String bpm, String pres) {
        this.Temperature = temp;
        this.bpm = bpm;
        this.pressure = pres;
    }

    public String getTemperature() {
        return Temperature;
    }

    public String getBpm() {
        return bpm;
    }

    public String getPressure() {
        return pressure;
    }

    // although it might seem like these getter functions are not in use, but they are actually used by Firebase for extracting the class data
    // for saving to the Firebase database
}
